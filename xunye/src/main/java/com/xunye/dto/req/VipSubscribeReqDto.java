package com.xunye.dto.req;
import jakarta.validation.constraints.*;
/**
 * 前端开通/续费VIP时提交的JSON参数；record自动生成构造方法和requestId()、expectedPrice()访问方法。
 * @param requestId 本次提交的唯一标识。@NotBlank禁止空值/空白，@Pattern限制为16到64位字母、数字、下划线或短横线。
 *                  网络异常后重试沿用同一标识；用户真正要再买一次时生成新标识，否则只会返回上次结果。
 * @param expectedPrice 用户在页面确认的套餐价格，单位分。@NotNull禁止null，@Positive要求大于0。
 *                      服务端会与数据库价格比较，不能用前端传入金额直接扣款。
 */
public record VipSubscribeReqDto(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{16,64}") String requestId,
    @NotNull @Positive Long expectedPrice) {}
