package com.jinyeong.couponservice.repository;

import com.jinyeong.couponservice.domain.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.couponPolicy.id = :policyId")
    Long countByCouponPolicyId(@Param("policyId") Long policyId);

    Page<Coupon> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Coupon.Status status, Pageable pageable);

    /**
     * [PESSIMISTIC_WRITE]
     *  - 비관적 락 (for update. 읽기 시점부터 하나의 트랜잭션만이 읽게끔 잠금)
     *  - 쿠폰 사용/취소처럼 조회 후 상태를 변경하는 작업에 사용한다.
     *  - 락이 없으면 두 트랜잭션이 동시에 AVAILABLE 상태를 읽고 각각 검증을 통과해, 하나의 쿠폰이 여러 주문에 중복 적용될 수 있다(lost update).
     *  - Coupon.use()의 상태 검증은 조회 시점의 스냅샷을 보는 것이라 이를 막지 못한다.
     *  - SELECT ... FOR UPDATE로 행 락을 걸어 뒤따라온 트랜잭션이 대기하도록 한다.
     *  - 같은 쿠폰에 요청이 몰리는 특성상 재시도가 잦은 낙관적 락보다 유리하다.
     * [주의]
     *  - 반드시 @Transactional 안에서 호출해야 하며, 락은 커밋/롤백 시점까지 유지되므로 트랜잭션을 짧게 유지할 것.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithLock(@Param("id") Long id);
}
