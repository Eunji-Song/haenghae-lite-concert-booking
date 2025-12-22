# Kafka 로컬 실행 및 기본 기능 테스트

본 문서는 Docker Compose 기반으로 로컬 Kafka 클러스터를 구성하고,
토픽 생성 → Producer 메시지 발행 → Consumer 메시지 수신까지
Kafka의 기본 동작을 검증한 과정을 정리한 문서

---

## 1. Kafka 로컬 실행 환경

### 1.1 사용 구성
- Zookeeper: 1대
- Kafka Broker: 3대 (broker1, broker2, broker3)
- Docker Compose 기반 실행
- 이미지: confluentinc/cp-zookeeper, confluentinc/cp-kafka

---

## 2. Kafka 클러스터 실행

### 2.1 Docker Compose 실행

```bash
docker-compose -f docker-compose.kafka.yml up -d
```

### 2.2 컨테이너 상태 확인

```bash
docker ps
```

정상적으로 아래 컨테이너가 실행 중인지 확인한다.
- zookeeper
- broker1
- broker2
- broker3

---

## 3. Kafka 토픽 생성 및 확인

### 3.1 broker1 컨테이너 접속

```bash
docker exec -it broker1 bash
```

### 3.2 토픽 목록 조회

```bash
kafka-topics \
  --bootstrap-server broker1:29092 \
  --list
```

### 3.3 토픽 생성

```bash
kafka-topics \
  --bootstrap-server broker1:29092 \
  --create \
  --topic test-topic \
  --replication-factor 1 \
  --partitions 3
```

> 이미 토픽이 존재할 경우 `Topic already exists` 오류가 발생할 수 있음

---

## 4. Producer 메시지 발행 테스트

### 4.1 Producer 실행

```bash
kafka-console-producer \
  --bootstrap-server broker1:29092 \
  --topic test-topic
```

### 4.2 메시지 입력

```text
hello kafka
week9 test
111
```

입력한 메시지는 Enter 기준으로 Kafka 토픽에 발행된다.

---

## 5. Consumer 메시지 수신 테스트

### 5.1 broker2 컨테이너 접속

```bash
docker exec -it broker2 bash
```

### 5.2 Consumer 실행

```bash
kafka-console-consumer \
  --bootstrap-server broker2:29093 \
  --topic test-topic \
  --from-beginning
```

### 5.3 결과 확인

Producer에서 발행한 메시지가 Consumer에서 정상적으로 출력되는 것을 확인한다.

```text
hello kafka
week9 test
```

이를 통해
- Kafka 클러스터가 정상적으로 구성되었고
- 서로 다른 브로커 간에도 메시지가 정상적으로 전달됨을 검증했다.

---

## 6. 테스트 정리

- Docker Compose 기반 Kafka 클러스터 로컬 실행 성공
- 토픽 생성 및 조회 정상 동작 확인
- Producer → Consumer 메시지 송수신 검증
- 서로 다른 브로커 간 메시지 전달 정상 확인