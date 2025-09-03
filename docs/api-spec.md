# 콘서트 예약 API Spec

**API** : 콘서트 예약 API
**Version** : v1
**Base URL** : localhost:8080

## 인증 방식

* 토큰 발급을 제외한 모든 API는 **JWT 기반 대기열 토큰**을 필요로 합니다.
* 클라이언트는 `X-Queue-Token: <JWT>` 헤더를 반드시 포함해야 합니다.
* 토큰은 `POST /queue/token` API를 통해 발급받습니다.

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
    <tr>
      <td rowspan="5">인증/대기열/일반</td>
      <td>UNAUTHORIZED(401)</td>
      <td>인증 실패, 토큰 누락 또는 유효하지 않은 토큰</td>
    </tr>
    <tr>
      <td>FORBIDDEN_TOKEN_INACTIVE(403)</td>
      <td>대기열 토큰은 있지만 아직 활성(Active) 상태가 아님</td>
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

<br>

---

# 예매 프로세스 흐름

본 API는 별도의 회원가입/로그인 과정이 없으며, 사용자가 **대기열에 입장하는 순간 암묵적으로 회원(User)이 생성**됩니다.  
따라서 **충전/결제/예약**의 모든 행위는 발급된 `userUuid` 및 `JWT 토큰`을 기반으로 수행됩니다.

## 전체 흐름
1. **대기열 입장**
  - `POST /queue/token`
  - 사용자 UUID 자동 발급 및 대기열 토큰(JWT) 반환

2. **나의 대기 순서 확인**
  - `GET /queue/me`
  - 사용자는 폴링으로 자신의 순서와 활성 상태(Active 여부)를 확인

3. **예약 가능 일자/좌석 조회**
  - `GET /dates`, `GET /dates/{date}/seats`
  - 사용자가 선택 가능한 날짜/좌석을 확인

4. **잔액 충전 (선택)**
  - `POST /balance/charge`
  - 회원가입은 없지만, 대기열 입장 시 발급된 UUID 기반으로 포인트 충전

5. **좌석 예약 요청 (임시 배정)**
  - `POST /reservations/request`
  - 좌석을 홀드(기본 5분 TTL) → 결제 필요

6. **결제 (예약 확정)**
  - `POST /payments`
  - 잔액 차감 후 좌석을 확정(CONFIRMED)
  - 결제 성공 시 대기열 토큰 만료

7. **예약 내역 조회**
  - `GET /reservations/{reservationId}`
  - 특정 예약 상태/좌석 정보를 확인

---

## 중요한 특징
- **회원가입/로그인 없음**: 최초 대기열 입장이 곧 “암묵적 가입”
- **잔액 충전 시점**: 대기열 입장 후 가능
- **동시성 보장**: 동일 좌석에 대해 하나의 예약만 확정되며, Redis TTL로 임시 배정 관리

---

# API 명세 

## API 목록 (빠른 이동)
- [1. 토큰 발급 (대기열 입장)](#api-1)
- [2. 나의 대기 순서 확인](#api-2)
- [3. 예약 가능 일자 조회](#api-3)
- [4. 예약 가능 좌석 목록 조회](#api-4)
- [5. 좌석 예약 요청](#api-5)
- [6. 잔액 조회](#api-6)
- [7. 잔액 충전](#api-7)
- [8. 결제 및 예약 완료 진행](#api-8)
- [9. 예약 내역 조회 (예약 번호로 조회)](#api-9)


<a id="api-1"></a>
## 1. 토큰 발급 (대기열 입장)

### 기본 정보 
- Method: POST 
- URL: http://localhost:8080/queue/token
- Description: 대기열 토큰 발급

### Headers
없음 (최초 진입이므로 토큰 미보유)

### Response Value 
| 필드       | 타입   | 설명                                  |
|------------|--------|---------------------------------------|
| userUuid   | string | 사용자 고유 식별자(UUID)               |
| token      | string | JWT 형식의 대기열 토큰                 |
| status     | string | ISSUED (대기 중) / ACTIVE (예약 가능)  |
| rank       | int    | 현재 대기열 순번 (0이 맨 앞)           |
| etaSeconds | int    | 예상 대기 시간(초)                     |


### Success Response
- Status: 200 OK
```
{
    "userUuid": "3d4c5e90-7f66-4e4a-9b4b-82a59b1c1a21",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "status": "ISSUED",
    "rank": 1240,
    "etaSeconds": 780
}
```

### Error Response
| 코드                     | 설명            |
|------------------------|---------------|
| 400 VALIDATION_ERROR   | 잘못된 UUID 형식   |
| 429 RATE_LIMITED       | 과도한 요청        |
| 503 NOT_AVAILABLE      | 대기열 발급 중지/점검  |


<a id="api-2"></a>
## 2. 나의 대기 순서 확인

### 기본 정보
- Method: GET
- URL: http://localhost:8080/queue/me
- Description: 현재 나의 대기열 상태 조회

### Headers
- `Authorization: Bearer <JWT>`

### Response Value
| 필드            | 타입    | 설명                                      |
|-----------------|---------|-------------------------------------------|
| status          | string  | ISSUED (대기 중) / ACTIVE (예약 가능) / EXPIRED (만료) |
| rank            | int     | 현재 대기열 순번 (0이 맨 앞, ACTIVE 시 0)  |
| etaSeconds      | int     | 예상 대기 시간(초)                          |
| activeCapacity  | int     | 동시에 예약 가능한 사용자 수                 |
| positionChanged | boolean | 이전 조회 이후 순번이 변했는지 여부           |

### Success Response
- Status: 200 OK
```json
{
  "status": "ISSUED",
  "rank": 37,
  "etaSeconds": 210,
  "activeCapacity": 5,
  "positionChanged": true
}
```

### Error Response
| 코드               | 설명                  |
|------------------|---------------------|
| 401 UNAUTHORIZED | 토큰 누락/만료/유효하지 않음 |
| 429 RATE_LIMITED | 폴링 요청 과다            |


<a id="api-3"></a>
## 3. 예약 가능 일자 조회

### 기본 정보
- Method: GET
- URL: http://localhost:8080/dates
- Description: 예약 가능한 일자 목록 조회

### Headers
- `Authorization: Bearer <JWT>`

### Response Value
| 필드  | 타입    | 설명                       |
|-------|---------|----------------------------|
| date  | string  | 예약 가능 날짜 (yyyy-MM-dd) |
| open  | boolean | 해당 날짜 예약 오픈 여부      |

### Success Response
- Status: 200 OK
```json
[
  { "date": "2025-09-01", "open": true },
  { "date": "2025-09-02", "open": false }
]
```

### Error Response
| 코드               | 설명                  |
|------------------|---------------------|
| 401 UNAUTHORIZED | 토큰 누락/만료/유효하지 않음 |


<a id="api-4"></a>
## 4. 예약 가능 좌석 목록 조회

### 기본 정보
- Method: GET
- URL: http://localhost:8080/dates/{date}/seats
- Description: 특정 날짜의 좌석 상태 조회

### Headers
- `Authorization: Bearer <JWT>`

### Response Value
| 필드      | 타입    | 설명                                |
|-----------|---------|-----------------------------------|
| seatNo    | int     | 좌석 번호 (1~50)                     |
| state     | string  | AVAILABLE / HELD / CONFIRMED       |
| holdUntil | string  | state=HELD일 경우 만료 시각(ISO8601) |

### Success Response
- Status: 200 OK

```  
[
  { "seatNo": 1, "state": "AVAILABLE" },
  { "seatNo": 2, "state": "HELD", "holdUntil": "2025-09-01T12:30:05Z" },
  { "seatNo": 3, "state": "CONFIRMED" }
]
```

### Error Response
| 코드                  | 설명              |
|---------------------|-----------------|
| 400 VALIDATION_ERROR| 잘못된 날짜 형식     |
| 404 NOT_FOUND       | 해당 날짜 오픈되지 않음 |


<a id="api-5"></a>
## 5. 좌석 예약 요청

### 기본 정보
- Method: POST
- URL: http://localhost:8080/reservations/request
- Description: 좌석 임시 배정(홀드) 요청 (기본 5분 유효)

### Headers
- `Authorization: Bearer <JWT>`

### Request
- Body(JSON)

  {
  "date": "2025-09-01",
  "seatNo": 12
  }

### Response Value
| 필드          | 타입   | 설명                         |
|---------------|--------|----------------------------|
| reservationId | int    | 임시 예약 ID                 |
| holdExpiresAt | string | 홀드 만료 시각(ISO8601), 예: 2025-09-01T12:30:05Z |

### Success Response
- Status: 200 OK

```  
{
  "reservationId": 9123,
  "holdExpiresAt": "2025-09-01T12:30:05Z"
}
```

### Error Response
| 코드                         | 설명                    |
|----------------------------|-----------------------|
| 409 SEAT_ALREADY_CONFIRMED | 좌석 이미 확정됨          |
| 409 SEAT_HELD_BY_OTHERS    | 다른 사용자가 홀드 중       |
| 400 VALIDATION_ERROR       | 좌석 번호 범위 초과/잘못된 날짜 |


<a id="api-6"></a>
## 6. 잔액 조회

### 기본 정보
- Method: GET
- URL: http://localhost:8080/balance
- Description: 사용자 잔액 조회

### Headers
- `Authorization: Bearer <JWT>`

### Response Value
| 필드    | 타입 | 설명     |
|---------|------|----------|
| balance | int  | 현재 잔액(원) |

### Success Response
- Status: 200 OK

```
{
  "balance": 150000
}
```

### Error Response
| 코드               | 설명                    |
|------------------|-------------------------|
| 401 UNAUTHORIZED | 토큰 누락/만료/유효하지 않음 |


<a id="api-7"></a>
## 7. 잔액 충전

### 기본 정보
- Method: POST
- URL: http://localhost:8080/balance/charge
- Description: 잔액 충전

### Headers
- `Authorization: Bearer <JWT>`
- `Idempotency-Key: <UUID>`

### Request
- Body(JSON)

```    
{
    "amount": 100000
}
```

### Response Value
| 필드    | 타입   | 설명            |
|---------|--------|-----------------|
| balance | int    | 충전 후 잔액(원)  |
| txnId   | string | 충전 거래 ID     |

### Success Response
- Status: 200 OK

```    
{
    "balance": 250000,
    "txnId": "chg_20250824_0001"
}
```

### Error Response
| 코드                                         | 설명                               |
|--------------------------------------------|------------------------------------|
| 400 VALIDATION_ERROR                       | 금액이 0 이하 또는 형식 오류               |
| 401 UNAUTHORIZED                           | 토큰 누락/만료/유효하지 않음               |
| 422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD | 동일 멱등 키로 다른 요청 바디 재사용 시 |



<a id="api-8"></a>
## 8. 결제 및 예약 완료 진행

### 기본 정보
- Method: POST
- URL: http://localhost:8080/payments
- Description: 임시 배정 좌석 결제 및 예약 확정

### Headers
- `Authorization: Bearer <JWT>`
- `Idempotency-Key: <UUID>`

### Request
- Body(JSON)

```    
{
    "reservationId": 9123,
    "amount": 99000
}
    ```

### Response Value
| 필드      | 타입  | 설명                 |
|-----------|-------|----------------------|
| paymentId | int   | 결제 ID               |
| confirmed | bool  | 예약 확정 여부 (true) |

### Success Response
- Status: 200 OK

```    
{
    "paymentId": 22100,
    "confirmed": true
}
    ```

### Error Response
| 코드                                         | 설명                          |
|--------------------------------------------|-------------------------------|
| 401 UNAUTHORIZED                           | 토큰 누락/만료/유효하지 않음        |
| 402 INSUFFICIENT_BALANCE                   | 잔액 부족                       |
| 409 RESERVATION_CONFLICT                   | 중복 결제/경합 발생                |
| 410 HOLD_EXPIRED                           | 홀드 만료(임시 배정 시간 경과)       |
| 422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD | 동일 멱등 키로 다른 요청 바디 재사용 |



<a id="api-9"></a>
## 9. 예약 내역 조회 (예약 번호로 조회)

### 기본 정보
- Method: GET
- URL: http://localhost:8080/reservations/{reservationId}
- Description: 예약 단건 조회

### Headers
- `Authorization: Bearer <JWT>`

### Response Value
| 필드          | 타입   | 설명                                   |
|---------------|--------|----------------------------------------|
| reservationId | int    | 예약 ID                                 |
| date          | string | 예약 날짜 (yyyy-MM-dd)                   |
| seatNo        | int    | 좌석 번호 (1~50)                         |
| status        | string | PENDING / CONFIRMED                    |
| holdExpiresAt | string | 홀드 만료 시각(선택적, PENDING일 때만 포함) |

### Success Response
- Status: 200 OK

```   
{
    "reservationId": 9123,
    "date": "2025-09-01",
    "seatNo": 12,
    "status": "CONFIRMED",
    "holdExpiresAt": "2025-09-01T12:30:05Z"
}
```
### Error Response
| 코드            | 설명          |
|---------------|---------------|
| 401 UNAUTHORIZED | 토큰 누락/만료/유효하지 않음 |
| 404 NOT_FOUND | 예약 ID 없음   |


<br>

---

# 문서 변경 이력
* v1.0.0 (2025-08-24): 초기 작성(대기열/좌석/예약/잔액/결제 명세 및 에러 표준화)


<br>