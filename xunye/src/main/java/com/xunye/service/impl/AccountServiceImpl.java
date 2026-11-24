package com.xunye.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xunye.dao.UserMapper;
import com.xunye.dto.req.AuthReqDto;
import com.xunye.dto.resp.*;
import com.xunye.entity.User;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.CaptchaService;
import com.xunye.util.JwtUtils;
import com.xunye.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;

/**
 * 账号业务：验证码校验、BCrypt密码处理、用户创建和JWT签发。
 * Controller负责参数校验与请求限流；本类只接受需要的注册/登录信息。
 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserMapper users;
    private final CaptchaService captcha;
    private final JwtUtils jwt;

    // BCrypt自带随机盐，同一密码多次encode得到的散列也可能不同，验证须用matches。
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 注册成功即登录：插入用户后返回JWT和脱敏资料。
     * 注册不会充值或授予作者/VIP身份，相关权限由后续业务管理。
     */
    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor=Exception.class)
    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public LoginRespDto register(AuthReqDto req) {
        captcha.verify(req.getCaptchaId(), req.getCaptcha());
        // BCrypt限制的是UTF-8字节数，不只是字符数；中文密码可能更早达到72字节。
        if (req.getPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BizException(ErrorCode.PARAM_ERROR,"密码UTF-8编码不能超过72字节");
        User u = new User();
        // 当前以手机号作为登录标识，同时填充表中必填的username字段。
        u.setPhone(req.getPhone()); u.setUsername(req.getPhone());
        // 未填昵称时用手机号后四位生成默认名，不公开完整手机号。
        u.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname().trim() : "书友"+req.getPhone().substring(7));
        u.setPassword(encoder.encode(req.getPassword()));
        u.setBalance(0L); u.setIsAuthor(0); u.setStatus(0); u.setIsDeleted(0);
        // 唯一索引才是并发防重复注册的最终保障，捕获冲突后返回可理解的提示。
        try {
            users.insert(u);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PARAM_ERROR,"手机号已注册，请直接登录");
        }
        return new LoginRespDto(jwt.createUserToken(u.getId()), UserRespDto.from(u));
    }
    /** 登录：先消费验证码，再验证密码与账号状态，最后签发一枚新token。 */
    @Override
    public LoginRespDto login(AuthReqDto req) {
        captcha.verify(req.getCaptchaId(), req.getCaptcha());
        if (req.getPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BizException(ErrorCode.PARAM_ERROR,"密码UTF-8编码不能超过72字节");
        User u = users.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone,req.getPhone()));
        // 统一提示“手机号或密码不正确”，不会直接向未登录请求透露账号是否存在。
        if (u == null || !encoder.matches(req.getPassword(),u.getPassword()))
            throw new BizException(ErrorCode.PARAM_ERROR,"手机号或密码不正确");
        if (!Integer.valueOf(0).equals(u.getStatus())) throw new BizException(ErrorCode.USER_BANNED);
        return new LoginRespDto(jwt.createUserToken(u.getId()),UserRespDto.from(u));
    }
}
