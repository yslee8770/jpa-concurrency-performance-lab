# JPA OneToMany 대량 처리 성능 실험 정리

## 실험 목적

JPA 환경에서 `@OneToMany` 연관관계를 가진 엔티티를  
**대량으로 삭제/교체(replace)** 해야 하는 상황에서,

- 어떤 전략이 **성능·메모리·안정성 측면에서 안전한지**
- 어떤 전략이 **규모 증가 시 위험해지는지**

를 **코드와 수치로 검증**한다.

> 단순히 “빠르다/느리다”가 아니라  
> **영속성 컨텍스트(PC)와 메모리 관점에서의 한계**를 확인하는 것이 목적이다.

---

## 공통 실험 환경

- Spring Boot + Spring Data JPA
- Hibernate
- H2 (in-memory, MySQL mode)
- 동일한 데이터셋 재사용
- 트랜잭션 단위 실행
- 로그 최소화 상태에서 측정  
  (SQL / bind / ActionQueue 로그 OFF)

> 로깅 오버헤드를 제거한 상태에서도  
> **메모리 사용량과 PC 크기 차이가 명확히 드러남**

---

## 데이터 모델

- `PurchaseOrder (1) : OrderLine (N)`
- `@OneToMany(mappedBy = "order", orphanRemoval = true)`
- Child 수: **1,000 / 5,000 / 10,000**
- 컬렉션 로딩 여부를 실험 축으로 분리
  - Loaded = Y / N

---

## 대량 DELETE 전략 비교

### 비교 전략
- **DELETE_BY_ORDER_ID**
  - JPQL bulk delete
  - 컬렉션 미접근
- **ORPHAN_REMOVAL**
  - 컬렉션 clear 기반 삭제

### 관찰 결과 요약

| 전략 | 특징 |
|---|---|
| DELETE_BY_ORDER_ID | SQL 수 적음, PC 사용량 안정 |
| ORPHAN_REMOVAL | 컬렉션 로딩 시 PC/Heap 사용량 급증 |

- 시간(ms)은 큰 차이가 없었으나
- **Heap 사용량 차이가 전략별로 명확히 갈림**

👉 **대량 삭제에서는 orphanRemoval이 구조적으로 불리함**을 확인

---

## replace(삭제 + 재삽입) 시나리오

### 비교 전략
- **ORPHAN_REMOVAL_REPLACE**
  - 컬렉션 교체 방식
- **DELETE_BY_ORDER_ID + INSERT_CHUNK**
  - bulk delete 후
  - `flush/clear` 주기 제어하며 재삽입

---

## 핵심 관찰 포인트

### ORPHAN_REMOVAL_REPLACE

- replace 과정에서
  - 삭제 대상
  - 신규 삽입 대상
  **모두 영속성 컨텍스트에 적재**
- child 수가 증가할수록
  - PC 크기 = child 수
  - Heap 사용량 급증
- 10,000 기준:
  - 힙 한계 근접
  - OOM 위험 패턴 확인

👉 **replace 시 orphanRemoval은 규모가 커질수록 위험**

---

### DELETE + INSERT + flushInterval

- 컬렉션을 전혀 건드리지 않음
- `flushInterval(100 / 500 / 1000)`로
  - PC 크기 제어 가능
  - Heap 사용량 선형 조절 가능
- `managedEntityCountPeak ≈ 0` 수준 유지

👉 **flushInterval은 성능 튜닝이 아니라 “메모리 안전장치”**

---

## flushInterval

- flushInterval ↓
  - flush 횟수 ↑
  - Heap 안정성 ↑
- flushInterval ↑
  - flush 횟수 ↓
  - Heap 사용량 ↑ (위험)

> 대량 replace 작업에서는  
> flushInterval을 통해 **영속성 컨텍스트 크기를 제한하는 것이 핵심**

---

## 결론

### 선택 방향
- **대량 DELETE**
  - `DELETE_BY_ORDER_ID` (bulk delete)
- **대량 REPLACE**
  - bulk delete + insert
  - `flush/clear` 주기 제어

### 피해야 할 경우
- 대량 replace에서
  - `orphanRemoval`
  - 컬렉션 기반 조작

### 주의 신호
- PC 크기가 child 수와 함께 증가
- Heap 사용량이 로그 설정과 무관하게 급증
- flush가 트랜잭션 끝에 1회만 발생

---

## 정리

이 실험을 통해:

- JPA에서 대량 처리는
  - “ORM이 알아서 해주겠지” 영역 x
- 영속성 컨텍스트 관리가 핵심
- flushInterval과 컬렉션 접근 여부가
  성능보다 안정성에 더 큰 영향을 미친다

를 실제 실행 결과로 확인

---

