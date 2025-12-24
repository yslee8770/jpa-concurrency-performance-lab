# JPA Fetch Strategy Lab
> QueryDSL + Fetch Join 전략을 **실무 시나리오 기반으로 수치화·검증**한 실험 모듈

---

##  목적

JPA에서 연관관계 조회 시 자주 쓰이는 전략들에 대해  
**“빠르다 / 느리다”가 아니라**, 다음 질문에 답하는 것이 목표

- fetch join은 언제 안전한가?
- 왜 컬렉션 fetch join이 위험해지는가?
- batch fetch는 어떤 상황에서 대안이 되는가?
- 페이징·정렬·필터가 있는 실제 화면 쿼리에서는 어떤 전략이 합리적인가?
- 연관 그래프가 깊어질 때, 병목은 DB가 아니라 애플리케이션 힙이 되는 지점은 어디인가?

---

## 실험 환경

- Spring Boot 3.x / JPA (Hibernate)
- QueryDSL
- H2 (MySQL mode)
- JVM Heap 측정 + Hibernate Statistics 기반 수치화
- 동일한 데이터 스케일에서 전략별 비교

---

##  도메인 구조 (요약)

Order
├─ Member (ManyToOne)
├─ Delivery (OneToOne)
├─ OrderLines (OneToMany)
│ └─ LineOptions (OneToMany)
└─ PaymentAttempts (OneToMany)


- **to-many 컬렉션 2개 이상 + depth 증가** 시  
  fetch join row 폭발이 발생하도록 의도적으로 설계

---

##  실험 목록

### Baseline — N+1 기준선

- to-one만 fetch join
- 컬렉션은 LAZY 접근
- 목적: “아무 전략 안 썼을 때” 기준 수치 확보

baseline | 368ms | heapΔ=12,584,960 | queries=1 | entityLoad=3200 | collFetch=400


---

### Fetch Join Row Explosion

#### (1) 컬렉션 1개 fetch join
- `Order → OrderLines`

#### (2) 컬렉션 2개 fetch join
- `Order → OrderLines`
- `Order → PaymentAttempts`

→ SQL은 **1번**이지만,  
→ 결과 row 수 증가로 **heap 사용량과 entity materialization 급증**

(실험 결과: 컬렉션 2개 fetch join 시 heap 사용량 급격히 증가)

---

### Batch Fetch 대안 전략

- to-one만 fetch join
- 컬렉션은 LAZY
- `hibernate.default_batch_fetch_size`로 IN 절 batching

toOneFetchJoin + batchFetch(lazy collections)
| 286ms | heapΔ=13,633,536 | queries=1 | entityLoad=5600 | collFetch=4


✔ row 폭발 없음  
✔ heap 사용 안정  
✔ 쿼리 수 증가 없음

---

### 실무 화면 쿼리 시나리오
**(페이징 + 정렬 + 필터)**

- 조건:
  - 회원명 prefix 필터
  - 결제 상태 필터
  - 최신순 정렬
- 전략:
  1. ID 페이징 쿼리
  2. ID 기준 to-one fetch join
  3. 컬렉션은 batch fetch

screen(ID page -> toOne fetchJoin + batch)
| 283ms | heapΔ=8,904,320 | queries=1 | entityLoad=5600 | collFetch=4


✔ 페이징 안정성 유지  
✔ fetch join row 폭발 회피  
✔ 실무 화면 요구사항 충족

---

### Join Depth 증가 시 Heap Pressure (OOM 재현)

- Order → Lines → Options → Payments
- fetch join으로 그래프 전체 로딩
- JVM heap 제한 시 `OutOfMemoryError` 발생
 

> 이 테스트는 `@Tag("oom")`으로 기본 테스트에서 제외  
> 별도 커맨드로만 실행

```bash
./gradlew :jpa-fetch-strategy-lab:oomTest
```
결론:
연관 그래프가 깊어질수록 병목은 DB가 아니라 애플리케이션 메모리가 된다.

---

## 요약
-  fetch join은 to-one 위주로 제한
-  컬렉션 fetch join은 여러 개 동시 사용 금지
-  컬렉션 로딩은 batch fetch(IN 절)로 대체
-  페이징·정렬·필터가 있으면
  → ID 페이징 → 2-step 로딩이 가장 안전
  → row 수 × entity materialization × heap 사용량이 병목
