문제가 되는 JPA / DB / 동시성 이슈를  
코드와 테스트로 재현·검증한 기술 실험 모음

각 실험은 모듈 단위로 분리되어 있으며,
상세 내용은 개별 모듈 README에 정리

---

## 

- [UPDATE 전략 실험)](./exp-onetomany-bulk-delete-flush-interval/README.md)

- [QueryDSL + Fetch 전략 실험 (fetch join · batch size · OOM)](./jpa-fetch-strategy-lab/README.md)

- [OneToMany 스케일 증가 실험](./jpa-onetomany-scale/README.md)

- [Optimistic Lock 재고 감소 실험 (retry 패턴)](./jpa-optimistic-lock-stock/README.md)

- [Pessimistic Lock Deadlock 회피 실험](./jpa-pessimistic-lock/README.md)
