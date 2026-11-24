package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 阅读偏好表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reader_setting")
public class ReaderSetting extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 字号，默认 18 */
    private Integer fontSize;

    /** 字体 */
    private String fontFamily;

    /** 文字颜色 */
    private String textColor;

    /** 阅读背景：white/beige/parchment/night */
    private String bgTheme;

    /** UI 主题：light/dark/vip */
    private String uiTheme;

    /** 行间距倍数 ×10（15 = 1.5 倍） */
    private Integer lineSpacing;
}
