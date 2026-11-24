package com.xunye.dto.req;
import jakarta.validation.constraints.*;
import lombok.Data;
/**
 * 登录/注册共用请求：phone为登录手机号，password为待加密/校验的原始密码。
 * captchaId对应Redis验证码键，captcha是图片答案；nickname仅注册时使用。
 * 本DTO仅用于接收入参，不能记录到日志或原样返回给前端。
 */
@Data
public class AuthReqDto {
    @NotBlank @Pattern(regexp="^1[3-9][0-9]{9}$", message="请输入正确的手机号")
    private String phone;
    @NotBlank @Size(min=8, max=64, message="密码须为8至64位")
    private String password;
    @NotBlank @Size(max=64)
    private String captchaId;
    @NotBlank @Pattern(regexp="[0-9]{4}", message="请输入4位图形验证码")
    private String captcha;
    @Size(max=30, message="昵称最多30字") private String nickname;
}
