import http from "k6/http";
import { check, sleep, fail } from "k6";
import { SharedArray } from "k6/data";
import { Rate, Counter } from "k6/metrics";

/**
 * 목적
 * - 동일 좌석(concertId/date/seatNo)에 대해 예약(hold) API를 집중 타격하여 동시성/락/예외 처리 품질 확인
 *
 * 호출 흐름
 * - 로그인(Setup에서 1회) -> 대기열 토큰 발급 -> (옵션) 대기열 ACTIVE 폴링 -> 예약(hold)
 *
 * 기대 응답(예약)
 * - 200: 홀드 성공
 * - 409 + code=SEAT002: 이선좌(이미 예약된 좌석) - 정상 케이스
 * - 기타(401/403/500 등): 비정상 케이스로 집계
 *
 * 실행 팁
 * - CLI에서 --vus/--iterations를 주면 options.scenarios가 덮어써지므로, 스트레스 시나리오 실행 시에는 주지 않음
 */

// ----------------------------
// 환경변수 (기본값 포함)
// ----------------------------
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

const CONCERT_ID = Number(__ENV.CONCERT_ID || "1");
const DATE = __ENV.DATE || "2025-12-01";
const SEAT_NO = Number(__ENV.SEAT_NO || "10");

const QUEUE_POLL = (__ENV.QUEUE_POLL || "false").toLowerCase() === "true";
const POLL_MAX = Number(__ENV.POLL_MAX || "10");
const POLL_SLEEP = Number(__ENV.POLL_SLEEP || "0.2");

const LOGIN_PATH = __ENV.LOGIN_PATH || "/api/v1/auth/login";
const SIGNUP_PATH = __ENV.SIGNUP_PATH || "/api/v1/auth/signup";
const AUTO_SIGNUP = (__ENV.AUTO_SIGNUP || "false").toLowerCase() === "true";

// ----------------------------
// 테스트 계정 (Setup에서 토큰 발급)
// ----------------------------
const USERS = new SharedArray("users", function () {
  return [
    { email: "loadTest01@test.com", password: "loadTest01Pw", name: "loadTest01" },
    { email: "loadTest02@test.com", password: "loadTest02Pw", name: "loadTest02" },
    { email: "loadTest03@test.com", password: "loadTest03Pw", name: "loadTest03" },
    { email: "loadTest04@test.com", password: "loadTest04Pw", name: "loadTest04" },
  ];
});

// ----------------------------
// 커스텀 메트릭
// ----------------------------
const reserve_success_200 = new Rate("reserve_success_200");
const reserve_seat_already_reserved = new Rate("reserve_seat_already_reserved"); // 409 + SEAT002
const reserve_auth_error = new Rate("reserve_auth_error"); // 401/403
const reserve_queue_error = new Rate("reserve_queue_error"); // 401/403 + QUEUE*
const reserve_sys001 = new Rate("reserve_sys001"); // 500 + SYS001
const reserve_unexpected = new Rate("reserve_unexpected"); // 그 외 예상 못한 응답
const unexpected_reserve_status = new Rate("unexpected_reserve_status"); // 임계치용(비정상 여부)

const reserve_status_count = new Counter("reserve_status_count"); // 상태코드 카운트(관측용)

// ----------------------------
// 시나리오/임계치
// ----------------------------
export const options = {
  scenarios: {
    seat_hold_stress: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "5s", target: 20 },
        { duration: "10s", target: 100 },
        { duration: "15s", target: 200 },
        { duration: "10s", target: 200 },
        { duration: "10s", target: 0 },
      ],
      gracefulRampDown: "5s",
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<1500"],
    // 좌석 경쟁에서는 200/409(SEAT002)이 대부분이어야 함
    unexpected_reserve_status: ["rate<0.01"],
    // 인증/대기열 문제는 사실상 0이어야 함
    reserve_auth_error: ["rate<0.001"],
    reserve_queue_error: ["rate<0.001"],
    // 서버 내부 오류는 사실상 0이어야 함
    reserve_sys001: ["rate<0.001"],
  },
};

// ----------------------------
// 공통 유틸
// ----------------------------
function jsonSafe(res) {
  try {
    return res.json();
  } catch (_) {
    return null;
  }
}

function jsonCodeSafe(res) {
  try {
    return res.json("code") || "";
  } catch (_) {
    return "";
  }
}

function pickTokenForVu(tokens) {
  if (!tokens || tokens.length === 0) fail("setup()에서 토큰이 전달되지 않았습니다.");
  const idx = (__VU - 1) % tokens.length;
  return tokens[idx];
}

// ----------------------------
// Auth
// ----------------------------
function signup(user) {
  const url = `${BASE_URL}${SIGNUP_PATH}`;
  const payload = JSON.stringify({ email: user.email, password: user.password, name: user.name });

  const res = http.post(url, payload, {
    headers: { "Content-Type": "application/json", accept: "*/*" },
    tags: { name: "auth_signup" },
  });

  check(res, {
    "signup status 200/201/400/409": (r) =>
      r.status === 200 || r.status === 201 || r.status === 400 || r.status === 409,
  });

  return res;
}

function login(user) {
  const url = `${BASE_URL}${LOGIN_PATH}`;
  const payload = JSON.stringify({ email: user.email, password: user.password });

  const res = http.post(url, payload, {
    headers: { "Content-Type": "application/json", accept: "*/*" },
    tags: { name: "auth_login" },
  });

  if (!res) fail("login 응답이 없습니다.");

  if (res.status !== 200) return null;

  const data = jsonSafe(res);
  const accessToken = data && data.accessToken;
  return accessToken || null;
}

// ----------------------------
// Queue
// ----------------------------
function issueQueueToken(authHeader) {
  const url = `${BASE_URL}/api/v1/queue/token`;
  const payload = JSON.stringify({ concertId: CONCERT_ID });

  const res = http.post(url, payload, {
    headers: {
      "Content-Type": "application/json",
      accept: "*/*",
      Authorization: authHeader,
    },
    tags: { name: "queue_issue" },
  });

  check(res, { "queue/token status 200": (r) => r.status === 200 });

  if (res.status !== 200) return null;

  const data = jsonSafe(res);
  return data && data.queueToken ? data.queueToken : null;
}

function waitQueueActive(authHeader, queueToken) {
  if (!QUEUE_POLL) return true;

  const url = `${BASE_URL}/api/v1/queue/me`;

  for (let i = 0; i < POLL_MAX; i++) {
    const res = http.get(url, {
      headers: {
        accept: "*/*",
        Authorization: authHeader,
        "X-Queue-Token": queueToken,
      },
      tags: { name: "queue_poll" },
    });

    if (res.status !== 200) return false;

    const data = jsonSafe(res);
    if (data && data.status === "ACTIVE") return true;

    sleep(POLL_SLEEP);
  }

  return false;
}

// ----------------------------
// Reservation
// ----------------------------
function reserveSameSeat(authHeader, queueToken) {
  const url = `${BASE_URL}/api/v1/reservations`;
  const payload = JSON.stringify({ concertId: CONCERT_ID, date: DATE, seatNo: SEAT_NO });

  const res = http.post(url, payload, {
    headers: {
      "Content-Type": "application/json",
      accept: "*/*",
      Authorization: authHeader,
      "X-Queue-Token": queueToken,
    },
    tags: { name: "reservation_hold" },
  });

  const status = res.status;
  const code = jsonCodeSafe(res);

  reserve_status_count.add(1, { status: String(status) });

  // 정상/기대 케이스
  const isOk200 = status === 200;
  const isSeatAlreadyReserved = status === 409 && code === "SEAT002";

  // 인증/권한/대기열 오류
  const isAuthError = status === 401 || status === 403;
  const isQueueError = isAuthError && (code.startsWith("QUEUE") || code === "QUEUE001" || code === "QUEUE004");

  // 서버 내부 오류
  const isSys001 = status === 500 && code === "SYS001";

  reserve_success_200.add(isOk200);
  reserve_seat_already_reserved.add(isSeatAlreadyReserved);
  reserve_auth_error.add(isAuthError);
  reserve_queue_error.add(isQueueError);
  reserve_sys001.add(isSys001);

  const isExpected = isOk200 || isSeatAlreadyReserved;

  // 예상 못한 케이스
  const isUnexpected = !isExpected;
  reserve_unexpected.add(isUnexpected);
  unexpected_reserve_status.add(isUnexpected);

  if (isUnexpected) {
    console.log(
      `[reserve][UNEXPECTED] status=${status} code=${code} body=${res.body}`
    );
  }

  check(res, {
    "reserve expected (200 or 409/SEAT002)": () => isExpected,
  });

  return status;
}

// ----------------------------
// setup(): 계정별 access token 발급(1회)
// ----------------------------
export function setup() {
  if (!USERS || USERS.length === 0) fail("USERS가 비어있습니다.");

  const tokens = [];

  for (const user of USERS) {
    let token = login(user);

    if (!token && AUTO_SIGNUP) {
      signup(user);
      token = login(user);
    }

    if (!token) {
      fail(`setup 로그인 실패: email=${user.email}`);
    }

    tokens.push(token);
  }

  return { tokens };
}

// ----------------------------
// VU 실행
// ----------------------------
export default function (data) {
  const token = pickTokenForVu(data.tokens);
  const authHeader = `Bearer ${token}`;

  const queueToken = issueQueueToken(authHeader);
  if (!queueToken) {
    sleep(0.1);
    return;
  }

  const active = waitQueueActive(authHeader, queueToken);
  if (!active) {
    sleep(0.1);
    return;
  }

  reserveSameSeat(authHeader, queueToken);

  sleep(0.05);
}