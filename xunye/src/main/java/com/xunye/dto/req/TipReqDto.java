package com.xunye.dto.req;
import jakarta.validation.constraints.*;
/** 金额单位为分，单次打赏限制为1分至1000元；同一次提交重试必须复用requestId。 */
public record TipReqDto(@NotNull @Min(1) @Max(com.xunye.constant.WalletConstant.MAX_TIP_FEN) Long amount,
    @Size(max=100) String message,
    @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{16,64}") String requestId) {}
