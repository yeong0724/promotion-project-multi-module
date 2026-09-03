package com.jinyeong.couponservice.service.v2;

import com.jinyeong.couponservice.aop.CouponMetered;
import com.jinyeong.couponservice.config.UserIdInterceptor;
import com.jinyeong.couponservice.domain.Coupon;
import com.jinyeong.couponservice.domain.CouponPolicy;
import com.jinyeong.couponservice.dto.v1.CouponDto;
import com.jinyeong.couponservice.exception.CouponIssueException;
import com.jinyeong.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final CouponPolicyService couponPolicyService;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final long LOCK_WAIT_TIME = 3;
    private static final long LOCK_LEASE_TIME = 5;

    @Transactional
    @CouponMetered(version = "v2")
    public Coupon issueCoupon(CouponDto.IssueRequest request) {
        String quantityKey = COUPON_QUANTITY_KEY + request.getCouponPolicyId();
        String lockKey = COUPON_LOCK_KEY + request.getCouponPolicyId();

        /**
         * [RLock]
         *  - Redis에서 제공하는 락 구현체로 Redisson의 분산 락
         *  - LOCK_WAIT_TIME: 락을 못 잡으면 최대 3초 대기, 그 안에 못 잡으면 false 반환
         *  - LOCK_LEASE_TIME: 락 획득 후 5초가 지나면 자동 해제. 프로세스가 죽어도 락이 영원히 남지 않게 하는 안전장치
         *
         *  -
         */
        RLock rLock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = rLock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new CouponIssueException("쿠폰 발급 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
            }

            CouponPolicy couponPolicy = couponPolicyService.getCouponPolicy(request.getCouponPolicyId());

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new IllegalStateException("쿠폰 발급 기간이 아닙니다.");
            }

            // 수량 체크 및 감소
            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
            long remainingQuantity = atomicQuantity.decrementAndGet();

            if (remainingQuantity < 0) {
                atomicQuantity.incrementAndGet();
                throw new CouponIssueException("쿠폰이 모두 소진되었습니다.");
            }

            // 쿠폰 발급
            Coupon newCoupon = Coupon.builder()
                    .couponPolicy(couponPolicy)
                    .userId(UserIdInterceptor.getCurrentUserId())
                    .couponCode(generateCouponCode())
                    .build();

            return couponRepository.save(newCoupon);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponIssueException("쿠폰 발급 중 오류가 발생했습니다.");
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }

    private String generateCouponCode() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
