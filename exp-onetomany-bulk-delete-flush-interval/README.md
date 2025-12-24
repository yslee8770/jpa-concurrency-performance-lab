# 대량 데이터 UPDATE 전략 비교 (JPA)

이 모듈은 **대량 데이터 UPDATE 상황에서 어떤 전략을 선택해야 하는지**를
단순 성능 비교가 아니라 **구조·위험·선택 기준 관점**에서 검증하기 위한 실험이다.

비교 대상은 다음 3가지다.

- Dirty Checking
- JPQL Bulk Update
- JDBC Batch Update

---

## 실험 환경

- Spring Boot + Spring Data JPA
- H2 (In-Memory, MySQL mode)
- 동일한 엔티티 / 동일한 데이터 범위 사용
- 각 전략 실행 전 데이터 상태 reset
- Query Count(datasource-proxy) 기반 계측

>  실행 시간(ms)은 환경 영향을 크게 받으므로 **참고값**으로만 사용하고,
>  실험의 핵심은 **쿼리 구조와 동작 방식 차이**에 있다.

---

## 1. Dirty Checking

### 방식
- 엔티티를 조회하여 상태 변경
- JPA 변경 감지 메커니즘에 의해 UPDATE 수행
- paging + flush/clear를 통해 대량 처리 상황을 고려

### 관찰 결과
- UPDATE 쿼리 다수 발생
- 엔티티 로딩 비용 + 영속성 컨텍스트 메모리 사용
- `@PreUpdate`, Auditing, 도메인 로직 **정상 동작**

### 특징
- 가장 **안전한 방식**
- 도메인 규칙과 이벤트가 중요한 경우 적합
- 대량 처리에서는 성능 한계 존재

---

## 2. JPQL Bulk Update

### 방식
- JPQL `update` 쿼리를 직접 실행
- 영속성 컨텍스트를 우회하여 DB에 바로 반영

### 관찰 결과
- UPDATE 쿼리 **1회**
- 가장 단순한 쿼리 구조
- 실행 시간은 데이터 수가 적을 경우 JDBC Batch보다 느릴 수 있음

### 주의 사항
- 영속성 컨텍스트와 **불일치(stale) 발생**
- `@PreUpdate`, Auditing, 엔티티 리스너 **미동작**
- 실행 후 반드시 `clear()` 필요

### 특징
- **전량 / 범위 기반** 대량 업데이트에 적합
- 도메인 로직이 개입되지 않는 경우에만 사용해야 함

---

## 3. JDBC Batch Update

### 방식
- JdbcTemplate의 `batchUpdate` 사용
- 애플리케이션 레벨에서 대상 ID를 직접 제어

### 관찰 결과
- batch size에 따라 UPDATE 쿼리 분할 발생
- 부분 업데이트(예: 1000건 중 10건 변경)에 유리
- 영속성 컨텍스트를 사용하지 않음

### 주의 사항
- JPA 생명주기 완전 우회
- Auditing / 엔티티 리스너 미동작

### 특징
- **선별/부분 업데이트**에 적합
- 성능과 대상 통제의 균형점
- 도메인 이벤트가 필요 없는 경우에 한해 사용

---

## 전략별 요약 비교

| 전략 | UPDATE 쿼리 수 | 도메인 안전성 | 부분 업데이트 | 비고 |
|----|----|----|----|----|
| Dirty Checking | 많음 | 높음 | 가능 | 가장 안전 |
| JPQL Bulk | 1 | 낮음 | 가능(조건) | 범위 처리 전용 |
| JDBC Batch | batch 단위 | 낮음 | 매우 적합 | 대상 통제 용이 |

---

## 결론 (선택 기준)

- **도메인 규칙·Auditing·이벤트가 중요하다면**
  → Dirty Checking
- **전량 / 범위 기반 대량 업데이트**
  → JPQL Bulk Update (+ clear 필수)
- **부분 / 선별 업데이트, 성능이 중요한 경우**
  → JDBC Batch Update

> “대량”의 기준은 단순한 건수보다  
> **변경 대상의 비율과 도메인 요구사항**

---

