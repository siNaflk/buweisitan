package com.xunye.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
/** M4持久化实体，响应通过DTO返回。 */
@Data @EqualsAndHashCode(callSuper=true) @TableName("user_notification")
public class UserNotification extends BaseEntity {
    private String eventId;
    private Long userId;
    private String title;
    private String content;
    private Integer isRead;
}
