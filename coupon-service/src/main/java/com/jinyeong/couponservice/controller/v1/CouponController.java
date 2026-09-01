package com.jinyeong.couponservice.controller.v1;

import com.jinyeong.couponservice.domain.Coupon;
import com.jinyeong.couponservice.dto.v1.CouponDto;
import com.jinyeong.couponservice.service.v1.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/issue")
    public ResponseEntity<CouponDto.Response> issueCoupon(@RequestBody CouponDto.IssueRequest issueRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CouponDto.Response.from(couponService.issueCoupon(issueRequest)));
    }

    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponDto.Response> useCoupon(
            @PathVariable Long couponId,
            @RequestBody CouponDto.UseRequest useRequest
    ) {
        return ResponseEntity.ok(CouponDto.Response.from(couponService.useCoupon(couponId, useRequest.getOrderId())));
    }

    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponDto.Response> cancelCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(CouponDto.Response.from(couponService.cancelCoupon(couponId)));
    }

    @GetMapping
    public ResponseEntity<List<CouponDto.Response>> getCoupons(
            @RequestParam(required = false) Coupon.Status status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        CouponDto.ListRequest request = CouponDto.ListRequest.builder()
                .status(status)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(couponService.getCoupons(request));
    }
}