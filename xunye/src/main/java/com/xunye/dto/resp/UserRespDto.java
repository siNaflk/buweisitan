package com.xunye.dto.resp;
import com.xunye.entity.User;
/**
 * 个人中心响应：maskedPhone为脱敏手机号，balance单位为分，vipExpireTime为空表示未开通。
 * 使用record限制响应字段，避免把User实体里的密码、完整手机号等意外序列化出去。
 */
public record UserRespDto(Long id, String nickname, String avatar, String maskedPhone,
                          Integer isAuthor, Long balance, java.time.LocalDateTime vipExpireTime) {
    public static UserRespDto from(User u) {
        String p = u.getPhone();
        return new UserRespDto(u.getId(), u.getNickname(), u.getAvatar(),
            p == null || p.length() < 7 ? "" : p.substring(0,3) + "****" + p.substring(p.length()-4),
            u.getIsAuthor(), u.getBalance(), u.getVipExpireTime());
    }
}
