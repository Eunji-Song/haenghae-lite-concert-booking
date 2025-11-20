## 프로젝트
콘서트 예약 서비스

## Getting Started

### Prerequisites

#### Running Docker Containers

`local` profile 로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행해주셔야 합니다.

```bash
docker-compose up -d
```

### Docs
- [API 명세서](./docs/openapi.json)
- [ERD](./docs/erd.md)
- [인프라 구성도](./docs/infra.md)


## 패키지 구조 
### 레이어드
```
{domain}/
 ├─ api/
 │   ├─ controller/   # @RestController: HTTP 엔드포인트
 │   ├─ dto/          # 요청/응답 DTO (검증/직렬화 어노테이션 허용)
 │   └─ mapper/       # (선택) DTO ↔ Domain 변환
 ├─ application/
 │   └─ service/      # @Service: 트랜잭션 경계, 도메인 오케스트레이션
 ├─ domain/
 │   ├─ model/        # 순수 도메인 모델(Entity/VO/규칙) — 프레임워크 의존 금지
 │   └─ repository/   # 도메인 리포지토리 인터페이스(추상)
 └─ infrastructure/
     └─ jpa/
         ├─ entity/       # JPA @Entity (DB 스키마 매핑)
         ├─ repository/   # Spring Data JPA 인터페이스
         ├─ adapter/      # (선택) 도메인 Repo 구현(Adapter)
         └─ mapper/       # Domain ↔ Entity 변환 (수동/MapStruct)
```



### 클린 
```
{clean-domain}/
 ├─ api/
 │   ├─ controller/       # REST 엔드포인트
 │   ├─ dto/              # 요청/응답 DTO (웹 전용)
 │   └─ mapper/           # DTO ↔ Command/Result
 ├─ application/
 │   ├─ usecase/          # 유스케이스 구현(Interactor) — @Service
 │   └─ port/
 │       ├─ in/           # 입력 포트 (Commands/Results 인터페이스 or DTO)
 │       │   ├─ commands/ # ReserveSeatCommand, PayForReservationCommand ...
 │       │   └─ results/  # ReservationResult, PaymentResult ...
 │       └─ out/          # 출력 포트 (Repository, 외부 서비스)
 ├─ domain/               # 순수 도메인(Reservation, SeatRef, Rules...)
 └─ infrastructure/
     └─ persistence/
         ├─ jpa/          # Entity/Repository/Adapter/Mapper
         └─ memory/       # 테스트 대역(InMemory*)
```