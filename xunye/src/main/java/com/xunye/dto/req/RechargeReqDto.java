package com.xunye.dto.req;
import jakarta.validation.constraints.*;
/** 请求只带档位与幂等号，入账金额必须由服务端读取档位。 */
public record RechargeReqDto(@NotNull @Positive Long planId,
    @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{16,64}") String requestId) {}
