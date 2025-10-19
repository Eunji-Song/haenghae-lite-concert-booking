# 콘서트 예약 API 명세서 

API : 콘서트 예약 API
Version : v1
Base URL : localhost:8080



## 공통 응답 규칙
- 모든 API는 HTTP Status Code를 기준으로 성공/실패 여부를 판단합니다.
- 200 OK: 요청 성공
- 201 Created: 리소스 생성 성공 (필요 시 Location 헤더 포함, 응답 본문 없음 가능)
- 204 No Content: 요청 성공, 응답 본문 없음
- 4xx: 클라이언트 오류 (잘못된 요청/인증 실패 등)
- 5xx: 서버 오류


## 에러코드 종류
<table>
  <thead>
    <tr>
      <th>Category</th>
      <th>Error Code</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <!-- 인증/대기열/일반 -->
    <tr>
      <td rowspan="10">인증/대기열/일반</td>
      <td>UNAUTHORIZED(401)</td>
      <td>인증 실패, 토큰 누락 또는 유효하지 않은 토큰</td>
    </tr>
    <tr>
      <td>TOKEN_EXPIRED(401)</td>
      <td>Access 토큰 또는 Queue 토큰이 만료됨</td>
    </tr>
    <tr>
      <td>TOKEN_REVOKED(401)</td>
      <td>로그아웃·강제 만료 등으로 폐기된 토큰 사용</td>
    </tr>
    <tr>
      <td>TOKEN_MISMATCH(400)</td>
      <td>Access 토큰과 Queue 토큰의 사용자/콘서트 정보 불일치</td>
    </tr>
    <tr>
      <td>FORBIDDEN_SCOPE(403)</td>
      <td>토큰에 필요한 권한/스코프가 없음</td>
    </tr>
    <tr>
      <td>FORBIDDEN_TOKEN_INACTIVE(403)</td>
      <td>대기열 토큰은 있지만 아직 활성(Active) 상태가 아님</td>
    </tr>
    <tr>
      <td>USER_NOT_FOUND(404)</td>
      <td>회원 정보가 존재하지 않음 (미가입, 탈퇴 계정)</td>
    </tr>
    <tr>
      <td>RATE_LIMITED(429)</td>
      <td>짧은 시간에 너무 많은 요청을 보냄</td>
    </tr>
    <tr>
      <td>VALIDATION_ERROR(400)</td>
      <td>요청 파라미터 값이 잘못되었거나 유효하지 않음</td>
    </tr>
    <tr>
      <td>NOT_FOUND(404)</td>
      <td>존재하지 않는 리소스 요청 (예: 잘못된 날짜, 예약 ID)</td>
    </tr>
    <!-- 좌석/예약 -->
    <tr>
      <td rowspan="4">좌석/예약</td>
      <td>SEAT_ALREADY_CONFIRMED(409)</td>
      <td>이미 다른 사용자가 해당 좌석을 확정 예약함</td>
    </tr>
    <tr>
      <td>SEAT_HELD_BY_OTHERS(409)</td>
      <td>해당 좌석이 다른 사용자에 의해 임시 배정(홀드)된 상태</td>
    </tr>
    <tr>
      <td>HOLD_EXPIRED(410)</td>
      <td>임시 배정(홀드) 시간이 만료되어 좌석 배정이 해제됨</td>
    </tr>
    <tr>
      <td>RESERVATION_CONFLICT(409)</td>
      <td>동시에 같은 좌석을 요청하여 충돌 발생</td>
    </tr>
    <!-- 결제/잔액 -->
    <tr>
      <td rowspan="3">결제/잔액</td>
      <td>INSUFFICIENT_BALANCE(402)</td>
      <td>결제에 필요한 잔액이 부족함</td>
    </tr>
    <tr>
      <td>PAYMENT_DUPLICATED(409)</td>
      <td>이미 동일한 결제가 처리된 상태 (중복 결제 요청)</td>
    </tr>
    <tr>
      <td>IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD(422)</td>
      <td>같은 멱등 키(Idempotency-Key)를 다른 요청 본문과 함께 재사용</td>
    </tr>
  </tbody>
</table>


## 인증 토큰 종류

### Access JWT (사용자 인증 토큰)
- 헤더: Authorization: Bearer <Access JWT>
- 모든 API 호출 시 필수 (사용자 식별 및 권한 검증)

### Queue JWT (대기열 토큰)
- 헤더: X-Queue-Token: <Queue JWT>
- 대기열 기반 API(예: /queue, /dates, /reservations, /payments 등) 호출 시 추가로 요구
- 대기열 상태/순번 검증에 사용됨

### 규칙
- 모든 API 요청에는 헤더에 반드시 Access JWT를 포함해야 함
- Queue JWT는 대기열 진입 및 예매 관련 API에서만 추가적으로 요구됨
- 개인 정보/잔액/내 정보 조회 API 등에는 Access JWT만 필요

## 예매 프로세스

### 예매 프로세스 특징 
- 회원가입/로그인 과정을 통해 사용자 계정을 생성/인증할 수 있습니다.
- 회원가입은 아직 계정이 없는 사용자에게만 제공됩니다.
- 로그인을 통해 Access JWT(사용자 인증 토큰)과 Refresh Token을 발급받습니다.
- 이후 사용자는 대기열에 입장하여 Queue JWT(대기열 토큰)를 추가로 받아야 예매 프로세스를 진행할 수 있습니다.
- 즉, 예매 관련 API 호출 시에는 Access JWT + Queue JWT 모두 필요합니다.
- 동일 좌석은 하나의 예약만 확정, Redis TTL로 홀드 관리 (동시성 보장)

### API 호출 플로우
1.	회원가입 (최초 1회)
- POST /api/v1/auth/signup
- 성공: 201 Created (본문 없음)
2.	로그인
- POST /api/v1/auth/login
- 응답: accessToken, refreshToken
- 이후 모든 API에 Authorization: Bearer <Access JWT> 적용
3.	(옵션) Access 만료 임박/만료 시 갱신
- POST /api/v1/auth/token/refresh
- 바디: { "refreshToken": "..." }
- 응답: 새 accessToken (+ 회전 시 새 refreshToken)
4.	대기열 입장 (콘서트 선택 후)
- POST /api/v1/queue/token
- 헤더: Authorization
- 바디: { "concertId": 456 }
- 응답: queueToken, status=ISSUED, rank, etaSeconds
5.	대기 상태 폴링 (활성화 대기)
- GET /api/v1/queue/me
- 헤더: Authorization, X-Queue-Token
- 응답: status 가 ACTIVE 가 될 때까지 주기적으로 호출
6.	예약 가능 날짜 조회
- GET /api/v1/concerts/{concertId}/dates
- 헤더: Authorization, X-Queue-Token
- 응답에서 open=true 날짜 선택
7.	예약 가능 좌석 조회
- GET /api/v1/concerts/{concertId}/dates/{date}/seats
- 헤더: Authorization, X-Queue-Token
- 응답의 state=AVAILABLE 좌석 선택
8.	좌석 배정(홀드) = 예약 리소스 생성
- 권장 URL(리소스 생성 모델):
POST /api/v1/reservations
- 헤더: Authorization, X-Queue-Token, Content-Type: application/json
- 바디: { "concertId": 456, "date": "2025-09-10", "seatNo": 12 }
- 응답: { "reservationId": 9123, "status": "PENDING", "holdExpiresAt": "..." }
9.	(옵션) 잔액 확인/충전
- 잔액 조회: GET /api/v1/wallet/balance (헤더: Authorization)
- 잔액 충전: POST /api/v1/wallet/charge
- 헤더: Authorization, Idempotency-Key: <UUID>
- 바디: { "amount": 100000 }
10.	결제 & 예약 확정
- POST /api/v1/payments
- 헤더: Authorization, X-Queue-Token, Idempotency-Key: <UUID>
- 바디: { "reservationId": 9123, "amount": 99000 }
- 응답: { "paymentId": 22100, "reservationId": 9123, "confirmed": true }
- 정책: 결제 성공 시 해당 콘서트 Queue 토큰 만료(추가 예매 방지 목적, 선택)
11.	예약 상세 확인
- GET /api/v1/reservations/{reservationId}
- 헤더: Authorization
- 응답: status=CONFIRMED, 좌석/일자/결제 금액 등
12.	(옵션) 내 예약 목록 확인
- GET /api/v1/reservations
- 헤더: Authorization
13.	(옵션) 로그아웃
- POST /api/v1/auth/logout
- 헤더: Authorization
- 바디: { "refreshToken": "..." }
- 응답: 204 No Content


## API 목록
- [1. 회원가입](#api-1)
- [2. 로그인](#api-2)
- [3. 로그아웃](#api-3)
- [4. 잔액 조회](#api-4)
- [5. 잔액 충전](#api-5)
- [6. 내 예약 내역 조회](#api-6)
- [7. 내 정보 조회](#api-7)
- [8. 대기열 입장](#api-8)
- [9. 예약 가능 날짜 조회](#api-9)
- [10. 예약 가능 좌석 조회](#api-10)
- [11. 좌석 배정(홀드)](#api-11)
- [12. 결제 및 예약 완료 진행](#api-12)
- [13. 예약 취소](#api-13)
- [14. 예약 상세 조회](#api-14)
- [15. Access Token Refresh](#api-15)
- [16. 대기 상태 폴링](#api-16)

<a id="api-1"></a>
### 1. 회원가입
- Method: POST
- URL: /api/v1/auth/signup
- Description: 이메일·비밀번호·이름으로 새 사용자 계정 생성. 이미 존재하는 이메일이면 409 반환.

**Headers**
- Content-Type: application/json
- (인증 불필요)

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd!",
  "name": "홍길동"
}
```

**규칙**
- email은 255자 이내
- password: 최소 8자(대문자/소문자/숫자/특수문자 중 2종 이상 권장)
- name: 1~50자

**Response**
- Status: 201 Created
- Body: 없음

**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td>요청 바디 형식 오류 (이메일 형식/비밀번호 정책/이름 길이 등)</td>
    </tr>
    <tr>
      <td>409</td>
      <td>EMAIL_ALREADY_EXISTS</td>
      <td>이미 가입된 이메일</td>
    </tr>
<tr>
      <td>400</td>
      <td>INVALID_EMAIL_FORMAT</td>
      <td>올바르지 않은 이메일 형식 입력</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 회원가입 시도</td>
    </tr>
  </tbody>
</table>

<br>


<a id="api-2"></a>
### 2. 로그인
- Method: POST
- URL: /api/v1/auth/login
- Description: 이메일/비밀번호로 사용자 인증 후 Access JWT 및 Refresh Token 발급.

**Headers**
- Content-Type: application/json
- (인증 불필요)

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd!"
}
```

**Response Body**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "accessTokenExpiresIn": 1800,
  "refreshToken": "rt_9b6d8d7c-1c7a-4f28-9f3c-7a0a9c1b0d11",
  "refreshTokenExpiresIn": 2592000
}
```

**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>accessToken</td>
      <td>string</td>
      <td>사용자 인증용 JWT (헤더: <code>Authorization: Bearer &lt;accessToken&gt;</code>)</td>
    </tr>
    <tr>
      <td>accessTokenExpiresIn</td>
      <td>int</td>
      <td>Access 토큰 만료 시간(초). 예: 1800(=30분)</td>
    </tr>
    <tr>
      <td>refreshToken</td>
      <td>string</td>
      <td>Access 재발급용 토큰(서버 저장/회전 권장)</td>
    </tr>
    <tr>
      <td>refreshTokenExpiresIn</td>
      <td>int</td>
      <td>Refresh 토큰 만료 시간(초). 예: 2592000(=30일)</td>
    </tr>
  </tbody>
</table>

**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td>요청 바디 형식 오류(이메일/비밀번호 누락·형식 불일치)</td>
    </tr>
    <tr>
      <td>401</td>
      <td>INVALID_EMAIL_FORMAT</td>
      <td>이메일 또는 비밀번호 불일치(보안상 권장 응답)</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 로그인 시도</td>
    </tr>
  </tbody>
</table>

<br>


<a id="api-3"></a>
### 3. 로그아웃 -> 리프레시 토큰을 받아야 하는 이유
- Method: POST
- URL: /api/v1/auth/logout
- Description: 현재 로그인 세션을 종료합니다. 

**Headers**
- Authorization: Bearer <Access JWT>
- Content-Type: application/json

**Request Body**
```json
{
  "refreshToken": "rt_9b6d8d7c-1c7a-4f28-9f3c-7a0a9c1b0d11"
}
```

**Response**
- Status: 204 No Content
- Body: 없음

**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td>요청 바디 형식 오류(잘못된 플래그/필드 누락 등)</td>
    </tr>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/서명 불일치/만료</td>
    </tr>
    <tr>
      <td>401</td>
      <td>TOKEN_REVOKED</td>
      <td>이미 폐기된 Refresh 토큰으로 로그아웃 시도</td>
    </tr>
    <tr>
      <td>401</td>
      <td>INVALID_REFRESH_TOKEN</td>
      <td>Refresh 토큰 형식 불일치 또는 서버 저장소에 없음</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 요청</td>
    </tr>
  </tbody>
</table>


<br>


<a id="api-4"></a>
### 4. 잔액 조회
- Method: GET
- URL: /api/v1/wallet/balance
- Description: 현재 로그인한 사용자의 지갑 잔액을 조회합니다. (읽기 전용, 멱등)

#### Headers
- Authorization: Bearer <Access JWT>
- Accept: application/json

#### Request Body
- 없음

#### Response Body
```json
{
  "balance": 150000,
  "currency": "KRW",
  "lastUpdatedAt": "2025-09-03T10:23:00"
}
```
**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>balance</td>
      <td>number</td>
      <td>현재 잔액(최소 0, 정수 원 단위 또는 소수 지원 시 소수점 포함)</td>
    </tr>
    <tr>
      <td>currency</td>
      <td>string</td>
      <td>통화 코드(예: <code>KRW</code>)</td>
    </tr>
    <tr>
      <td>lastUpdatedAt</td>
      <td>string</td>
      <td>잔액이 마지막으로 변경된 시각(ISO8601, KST)</td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/서명 불일치/만료</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 요청</td>
    </tr>
  </tbody>
</table>


<br>




<a id="api-5"></a>
### 5. 잔액 충전
- Method: POST
- URL: /api/v1/wallet/charge
- Description: 현재 사용자의 지갑 잔액을 충전합니다. Idempotency-Key로 중복 방지.

**Headers**
- Authorization: Bearer <Access JWT>
- Content-Type: application/json
- Idempotency-Key: <UUID> (필수)

**Request Body**
```json
{
  "amount": 100000
}
```

**Response Body**
```json
{
"balance": 250000,
"txnId": "chg_20250903_0001"
}
```

**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>balance</td>
      <td>number</td>
      <td>충전 후 잔액</td>
    </tr>
    <tr>
      <td>txnId</td>
      <td>string</td>
      <td>충전 거래 ID</td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td>금액이 0 이하이거나 요청 형식 오류</td>
    </tr>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
    <tr>
      <td>422</td>
      <td>IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD</td>
      <td>같은 멱등 키를 다른 요청 본문과 함께 재사용</td>
    </tr>
  </tbody>
</table>


<br>

<a id="api-6"></a>
### 6. 내 예약 내역 조회
- Method: GET
- URL: /api/v1/reservations
- Description: 현재 사용자의 모든 예약 내역을 조회합니다. 예약이 없는 경우 빈 배열 리턴

#### Headers
- Authorization: Bearer <Access JWT>

#### Request Body
- 없음

#### Response Body
```json
[
  {
    "reservationId": 9123,
    "concertId": 456,
    "date": "2025-09-10",
    "seatNo": 12,
    "status": "CONFIRMED",
    "paidAmount": 99000
  }
]
```

**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>reservationId</td>
      <td>int</td>
      <td>예약 ID</td>
    </tr>
    <tr>
      <td>concertId</td>
      <td>int</td>
      <td>콘서트 ID</td>
    </tr>
    <tr>
      <td>date</td>
      <td>string</td>
      <td>예약 날짜 (yyyy-MM-dd)</td>
    </tr>
    <tr>
      <td>seatNo</td>
      <td>int</td>
      <td>좌석 번호</td>
    </tr>
    <tr>
      <td>status</td>
      <td>string</td>
      <td>PENDING / CONFIRMED / CANCELED</td>
    </tr>
    <tr>
      <td>paidAmount</td>
      <td>number</td>
      <td>결제 금액</td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
  </tbody>
</table>

<br>


<a id="api-7"></a>
### 7. 내 정보 조회
- Method: GET
- URL: /api/v1/users/me 
- Description: 현재 로그인한 사용자의 계정/프로필 정보를 조회합니다.

**Headers**
- Authorization: Bearer <Access JWT>

**Request Body**
- 없음

**Response Body**
```json
{
  "userUuid": "3d4c5e90-7f66-4e4a-9b4b-82a59b1c1a21",
  "email": "user@example.com",
  "name": "홍길동",
  "createdAt": "2025-09-01T09:00:00"
}
```

**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>userUuid</td>
      <td>string</td>
      <td>사용자 고유 식별자(UUID)</td>
    </tr>
    <tr>
      <td>email</td>
      <td>string</td>
      <td>사용자 이메일</td>
    </tr>
    <tr>
      <td>name</td>
      <td>string</td>
      <td>사용자 이름</td>
    </tr>
    <tr>
      <td>createdAt</td>
      <td>string</td>
      <td>계정 생성 시각(ISO8601, KST)</td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
    <tr>
      <td>404</td>
      <td>USER_NOT_FOUND</td>
      <td>회원 정보가 존재하지 않음 (탈퇴/삭제된 계정 등)</td>
    </tr>
  </tbody>
</table>


<br>


<a id="api-8"></a>
### 8. 대기열 입장
- Method: POST
- URL: /api/v1/queue/token
- Description: 특정 콘서트 대기열에 입장하여 Queue JWT를 발급합니다. (사용자는 이후 예매 관련 API 호출 시 Queue JWT를 함께 전송)

**Headers**
- Authorization: Bearer <Access JWT>
- Content-Type: application/json

**Request Body**
```json
{
  "concertId": 456
}
```

**Response Body**
```json
{
"queueToken": "eyJhbGciOiJIUzI1NiIs...",
"status": "ISSUED",
"rank": 1240,
"etaSeconds": 780
}
```

**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>queueToken</td>
      <td>string</td>
      <td>대기열 JWT. 이후 예매 관련 API에서 <code>X-Queue-Token</code> 헤더로 전송</td>
    </tr>
    <tr>
      <td>status</td>
      <td>string</td>
      <td>대기 상태. <code>ISSUED</code>(대기 중) / <code>ACTIVE</code>(입장 가능)</td>
    </tr>
    <tr>
      <td>rank</td>
      <td>int</td>
      <td>현재 대기 순번 (0이 맨 앞)</td>
    </tr>
    <tr>
      <td>etaSeconds</td>
      <td>int</td>
      <td>예상 대기 시간(초)</td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td><code>concertId</code> 누락/형식 오류</td>
    </tr>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
    <tr>
      <td>403</td>
      <td>QUEUE_CLOSED</td>
      <td>해당 콘서트 대기열이 닫힘/점검 중</td>
    </tr>
    <tr>
      <td>409</td>
      <td>ALREADY_IN_QUEUE</td>
      <td>해당 콘서트에 이미 대기열 토큰 발급됨</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 대기열 입장 요청</td>
    </tr>
  </tbody>
</table>



<br>

<a id="api-9"></a>
### 9. 예약 가능 날짜 조회
- Method: GET
- URL: /api/v1/concerts/{concertId}/dates
- Description: 특정 콘서트의 예약 가능 날짜 목록을 조회합니다.

#### Headers
- Authorization: Bearer <Access JWT>
- X-Queue-Token: <Queue JWT>

#### Path Params
- concertId (int) : 콘서트 식별자

#### Request Body
- 없음

#### Response Body
```json
[
  { "date": "2025-09-10", "open": true },
  { "date": "2025-09-11", "open": false }
]
```


**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>date</td>
      <td>string</td>
      <td>예약 날짜 (형식: yyyy-MM-dd)</td>
    </tr>
    <tr>
      <td>open</td>
      <td>boolean</td>
      <td>해당 날짜가 현재 예약 오픈 상태인지 여부</td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
    <tr>
      <td>403</td>
      <td>FORBIDDEN_TOKEN_INACTIVE</td>
      <td>대기열 토큰은 있으나 아직 활성(Active) 상태가 아님</td>
    </tr>
    <tr>
      <td>404</td>
      <td>NOT_FOUND</td>
      <td>존재하지 않는 <code>concertId</code></td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 요청</td>
    </tr>
  </tbody>
</table>


<br>

<a id="api-10"></a>

### 10. 예약 가능 좌석 조회
- Method: GET
- URL: /api/v1/concerts/{concertId}/dates/{date}/seats
- Description: 특정 콘서트의 특정 날짜에 대해 좌석 상태 목록(AVAILABLE/HELD/CONFIRMED) 을 조회합니다.

**Headers**
- Authorization: Bearer <Access JWT>
- X-Queue-Token: <Queue JWT>

**Path Params**
- concertId (int): 콘서트 식별자
- date (string): 날짜 (`yyyy-MM-dd`)

**Request Body**
- 없음

**Response Body**
```json
[
  { "seatNo": 1, "state": "AVAILABLE" },
  { "seatNo": 2, "state": "HELD"},
  { "seatNo": 3, "state": "CONFIRMED" }
]
```
**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>seatNo</td>
      <td>int</td>
      <td>좌석 번호</td>
    </tr>
    <tr>
      <td>state</td>
      <td>string</td>
      <td>좌석 상태: <code>AVAILABLE(선택가능)</code> / <code>HELD(홀드)</code> / <code>CONFIRMED(결제완료)</code></td>
    </tr>
  </tbody>
</table>



**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td><code>date</code> 형식 오류 또는 범위 초과</td>
    </tr>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
    <tr>
      <td>403</td>
      <td>FORBIDDEN_TOKEN_INACTIVE</td>
      <td>대기열 토큰 비활성(Active 아님)</td>
    </tr>
    <tr>
      <td>404</td>
      <td>NOT_FOUND</td>
      <td>존재하지 않는 <code>concertId</code> 또는 해당 날짜 미오픈</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 요청</td>
    </tr>
  </tbody>
</table>


<br>

<a id="api-11"></a>
### 11. 좌석 배정(홀드)
- Method: POST
- URL: `/api/v1/reservations/hold`
- Description: 지정한 콘서트/일자/좌석을 임시 배정(홀드) 합니다. 홀드는 제한 시간(TTL) 이후 자동 해제됩니다.

**Headers**
- Authorization: Bearer <Access JWT>
- X-Queue-Token: <Queue JWT>
- Content-Type: application/json

**Request Body**
```json
{
  "concertId": 456,
  "date": "2025-09-10",
  "seatNo": 12
}
```
**Response Body**
```json
{
"reservationId": 9123,
"holdExpiresAt": "2025-09-10T12:30:05Z"
}
```

**Response Value**

<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>reservationId</td>
      <td>int</td>
      <td>임시 배정(홀드) 식별자 ID</td>
    </tr>
    <tr>
      <td>holdExpiresAt</td>
      <td>string</td>
      <td>홀드 만료 시각(ISO8601, KST). 만료 시 좌석은 자동 해제됨</td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td><code>concertId</code>/<code>date</code>/<code>seatNo</code> 형식 오류 또는 범위 초과</td>
    </tr>
    <tr>
      <td>400</td>
      <td>TOKEN_MISMATCH</td>
      <td>Access/Queue 토큰의 사용자 또는 콘서트 스코프 불일치</td>
    </tr>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
    <tr>
      <td>403</td>
      <td>FORBIDDEN_TOKEN_INACTIVE</td>
      <td>대기열 토큰이 활성(Active) 상태가 아님</td>
    </tr>
    <tr>
      <td>404</td>
      <td>NOT_FOUND</td>
      <td>존재하지 않는 콘서트/일자/좌석 또는 미오픈 일자</td>
    </tr>
    <tr>
      <td>409</td>
      <td>SEAT_HELD_BY_OTHERS</td>
      <td>해당 좌석이 이미 다른 사용자에 의해 홀드된 상태</td>
    </tr>
    <tr>
      <td>409</td>
      <td>SEAT_ALREADY_CONFIRMED</td>
      <td>해당 좌석이 이미 확정 예약됨</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 홀드 요청</td>
    </tr>
  </tbody>
</table>


<br>

<a id="api-12"></a>
### 12. 결제 및 예약 완료 진행
- Method: POST
- URL: /api/v1/payments
- Description: 홀드(PENDING) 상태의 예약에 대해 결제를 수행하고 좌석을 확정(CONFIRMED) 합니다. 멱등 보장을 위해 Idempotency-Key 사용을 요구합니다.

**Headers**
- Authorization: Bearer <Access JWT>
- X-Queue-Token: <Queue JWT>
- Content-Type: application/json
- Idempotency-Key: <UUID>

**Request Body**
```json
{
  "reservationId": 9123,
  "amount": 99000
}
```

**Response Body**
```json
{
  "paymentId": 22100,
  "reservationId": 9123,
  "confirmed": true
}
```

**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>paymentId</td>
      <td>int</td>
      <td>결제 트랜잭션 ID</td>
    </tr>
    <tr>
      <td>reservationId</td>
      <td>int</td>
      <td>대상 예약 ID</td>
    </tr>
    <tr>
      <td>confirmed</td>
      <td>boolean</td>
      <td>예약 확정 여부(성공 시 <code>true</code>)</td>
    </tr>
  </tbody>
</table>


**Errors**

<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td><code>reservationId</code>/<code>amount</code> 누락·형식 오류</td>
    </tr>
    <tr>
      <td>400</td>
      <td>TOKEN_MISMATCH</td>
      <td>Access/Queue 토큰의 사용자 또는 콘서트 스코프 불일치</td>
    </tr>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
    <tr>
      <td>402</td>
      <td>INSUFFICIENT_BALANCE</td>
      <td>결제에 필요한 잔액 부족</td>
    </tr>
    <tr>
      <td>403</td>
      <td>FORBIDDEN_TOKEN_INACTIVE</td>
      <td>Queue 토큰 비활성(Active 아님)</td>
    </tr>
    <tr>
      <td>404</td>
      <td>NOT_FOUND</td>
      <td>예약 ID가 존재하지 않음 또는 이미 만료된 홀드</td>
    </tr>
    <tr>
      <td>409</td>
      <td>RESERVATION_CONFLICT</td>
      <td>중복 결제/동시 확정 경합(이미 확정/취소 상태 등)</td>
    </tr>
    <tr>
      <td>410</td>
      <td>HOLD_EXPIRED</td>
      <td>홀드 TTL 만료로 결제 불가</td>
    </tr>
    <tr>
      <td>422</td>
      <td>IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD</td>
      <td>같은 멱등 키를 다른 요청 본문과 함께 재사용</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 결제 요청</td>
    </tr>
  </tbody>
</table>

<br>

<a id="api-13"></a>
### 13. 예약 취소
- Method: DELETE
- URL: /api/v1/reservations/{reservationId}
- Description: 사용자가 자신의 예약을 취소합니다. (확정 상태의 환불/취소 정책은 비즈니스 규칙에 따름)

**Headers**
- Authorization: Bearer <Access JWT>

**Path Params**
- reservationId (int): 취소할 예약 ID

**Request Body**
- 없음

**Response Body**
```json
{ "canceled": true }
```

**Response Value**

<table>
  <thead>
    <tr><th>필드</th><th>타입</th><th>설명</th></tr>
  </thead>
  <tbody>
    <tr><td>canceled</td><td>boolean</td><td>취소 성공 여부</td></tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr><th>Code</th><th>Name</th><th>설명</th></tr>
  </thead>
  <tbody>
    <tr><td>401</td><td>UNAUTHORIZED</td><td>Access 토큰 누락/만료/유효하지 않음</td></tr>
    <tr><td>403</td><td>FORBIDDEN_SCOPE</td><td>본인 소유가 아닌 예약 취소 시도</td></tr>
    <tr><td>404</td><td>NOT_FOUND</td><td>해당 예약이 존재하지 않음</td></tr>
    <tr><td>409</td><td>RESERVATION_CONFLICT</td><td>이미 취소/환불 진행 중 또는 취소 불가 상태</td></tr>
  </tbody>
</table>



<br>


<a id="api-14"></a>
### 14. 예약 상세 조회
- Method: GET
- URL: /api/v1/reservations/{reservationId}
- Description: 특정 예약의 상세 정보를 조회합니다.

**Headers**
- Authorization: Bearer <Access JWT>

**Path Params**
- reservationId (int): 조회할 예약 ID

**Request Body**
- 없음

**Response Body**
```json
{
  "reservationId": 9123,
  "concertId": 456,
  "date": "2025-09-10",
  "seatNo": 12,
  "status": "CONFIRMED",
  "paidAmount": 99000,
  "holdExpiresAt": null,
  "createdAt": "2025-09-03T10:10:00Z",
  "updatedAt": "2025-09-03T10:12:00Z"
}
```


**Response Value**
<table>
  <thead>
    <tr><th>필드</th><th>타입</th><th>설명</th></tr>
  </thead>
  <tbody>
    <tr><td>reservationId</td><td>int</td><td>예약 ID</td></tr>
    <tr><td>concertId</td><td>int</td><td>콘서트 ID</td></tr>
    <tr><td>date</td><td>string</td><td>예약 날짜 (yyyy-MM-dd)</td></tr>
    <tr><td>seatNo</td><td>int</td><td>좌석 번호</td></tr>
    <tr><td>status</td><td>string</td><td>PENDING / CONFIRMED / CANCELED</td></tr>
    <tr><td>paidAmount</td><td>number</td><td>결제 금액(미결제 시 0 또는 null)</td></tr>
    <tr><td>holdExpiresAt</td><td>string</td><td>PENDING일 때 홀드 만료 시각(ISO8601), 확정/취소 시 null</td></tr>
    <tr><td>createdAt</td><td>string</td><td>예약 생성 시각(ISO8601, KST)</td></tr>
    <tr><td>updatedAt</td><td>string</td><td>예약 최종 갱신 시각(ISO8601, KST)</td></tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr><th>Code</th><th>Name</th><th>설명</th></tr>
  </thead>
  <tbody>
    <tr><td>401</td><td>UNAUTHORIZED</td><td>Access 토큰 누락/만료/유효하지 않음</td></tr>
    <tr><td>403</td><td>FORBIDDEN_SCOPE</td><td>본인 소유가 아닌 예약 상세 조회 시도</td></tr>
    <tr><td>404</td><td>NOT_FOUND</td><td>해당 예약이 존재하지 않음</td></tr>
  </tbody>
</table>



<br>

<a id="api-15"></a>
### 15. Access Token Refresh
- Method: POST
- URL: /api/v1/auth/token/refresh
- Description: 유효한 Refresh 토큰으로 새로운 Access 토큰을 발급합니다. (회전 전략 사용 시 **새 Refresh 토큰**도 함께 반환)

#### Headers
- Content-Type: application/json
- (인증 불필요 — Refresh 토큰으로만 인증)

#### Request Body
```json
{
  "refreshToken": "rt_9b6d8d7c-1c7a-4f28-9f3c-7a0a9c1b0d11"
}
```


**Response Body**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "accessTokenExpiresIn": 1800,
  "refreshToken": "rt_new_7a0a9c1b0d11",
  "refreshTokenExpiresIn": 2592000
}
```

**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>accessToken</td>
      <td>string</td>
      <td>새로 발급된 Access JWT</td>
    </tr>
    <tr>
      <td>accessTokenExpiresIn</td>
      <td>int</td>
      <td>Access 토큰 만료까지 남은 시간(초)</td>
    </tr>
    <tr>
      <td>refreshToken</td>
      <td>string</td>
      <td>(선택) 회전 전략일 때 새 Refresh 토큰</td>
    </tr>
    <tr>
      <td>refreshTokenExpiresIn</td>
      <td>int</td>
      <td>(선택) 새 Refresh 토큰 만료까지 남은 시간(초)</td>
    </tr>
    <tr>
      <td>tokenType</td>
      <td>string</td>
      <td>항상 <code>Bearer</code></td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td>요청 형식 오류(필드 누락 등)</td>
    </tr>
    <tr>
      <td>401</td>
      <td>INVALID_REFRESH_TOKEN</td>
      <td>형식/서명 불일치 또는 서버 저장소에 존재하지 않음</td>
    </tr>
    <tr>
      <td>401</td>
      <td>TOKEN_EXPIRED</td>
      <td>Refresh 토큰 만료</td>
    </tr>
    <tr>
      <td>401</td>
      <td>TOKEN_REVOKED</td>
      <td>폐기된 Refresh 토큰 사용(재사용 탐지 포함)</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 재발급 요청</td>
    </tr>
  </tbody>
</table>

<a id="api-16"></a>

### 15. 대기 상태 폴링 (활성화 대기)

- Method: GET
- URL: /api/v1/queue/me
- Description: 사용자가 발급받은 대기열 토큰(X-Queue-Token)의 현재 상태를 조회. 클라이언트는 일정 주기로 이 API를 호출하여 자신의 상태가 ACTIVE로 전환되었는지 확인

#### Headers

- Content-Type: application/json
- Authorization: Bearer <Access JWT> (사용자 인증)
- X-Queue-Token: <Queue JWT> (대기열 토큰, 필수)

#### Request Body

**Response Body**

```json
{
  "status": "QUEUED",
  "rank": 12,
  "etaSeconds": 180,
  "remainingSeconds": null
}
```

**Response Value**
<table>
  <thead>
    <tr>
      <th>필드</th>
      <th>타입</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>queueToken</td>
      <td>string</td>
      <td>대기열 JWT. 이후 예매 관련 API에서 <code>X-Queue-Token</code> 헤더로 전송</td>
    </tr>
    <tr>
      <td>status</td>
      <td>string</td>
      <td>대기 상태. <code>ISSUED</code>(대기 중) / <code>ACTIVE</code>(입장 가능)</td>
    </tr>
    <tr>
      <td>rank</td>
      <td>int</td>
      <td>현재 대기 순번 (0이 맨 앞)</td>
    </tr>
    <tr>
      <td>etaSeconds</td>
      <td>int</td>
      <td>예상 대기 시간(초)</td>
    </tr>
  </tbody>
</table>


**Errors**
<table>
  <thead>
    <tr>
      <th>Code</th>
      <th>Name</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>400</td>
      <td>VALIDATION_ERROR</td>
      <td><code>concertId</code> 누락/형식 오류</td>
    </tr>
    <tr>
      <td>401</td>
      <td>UNAUTHORIZED</td>
      <td>Access 토큰 누락/만료/유효하지 않음</td>
    </tr>
    <tr>
      <td>401</td>
      <td>INVALID_QUEUE_TOKEN</td>
      <td>잘못된 대기열 토큰</td>
    </tr>
    <tr>
      <td>403</td>
      <td>QUEUE_NOT_ACTIVE
OSED</td>
      <td>아직 ACTIVE 상태가 아님 (status=QUEUED)
</td>
    </tr>
    <tr>
      <td>409</td>
      <td>QUEUE_EXPIRED</td>
      <td>대기열 토큰이 만료됨</td>
    </tr>
    <tr>
      <td>429</td>
      <td>RATE_LIMITED</td>
      <td>짧은 시간 내 과도한 대기열 입장 요청</td>
    </tr>
  </tbody>
</table>

