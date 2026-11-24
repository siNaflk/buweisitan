package com.xunye.dto.req;
import jakarta.validation.constraints.*;
/** 确认页面看到的价格；价格变化时要求重新确认，不能按客户端价格扣款。 */
public record BuyChapterReqDto(@NotNull @PositiveOrZero Long expectedPrice) {}
