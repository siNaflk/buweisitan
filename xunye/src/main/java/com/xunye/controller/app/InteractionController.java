package com.xunye.controller.app;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.common.*;
import com.xunye.dto.req.*;
import com.xunye.dto.resp.*;
import com.xunye.service.*;
import com.xunye.util.AuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评论与论坛接口：公开读取路径为/comments、/forum/**；写操作及“我的帖子”在/interaction下。
 * WebConfig统一保护/interaction/**，操作人ID只从UserContext获取，不接受前端指定userId。
 * 路径中的id是资源ID（评论/帖子/回复），不是当前登录人ID。
 */
// @RestController：注册为Spring控制器，方法返回值由框架序列化成JSON响应。
@RestController
// @RequestMapping：设置这个控制器的公共路径前缀；完整接口路径还要拼接方法上的映射。
@RequestMapping("/api/app")
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService service;
    private final AuthHelper auth;
    private final CaptchaService rate;

    /** 游客可读；chapterId为空查书评，非空查章评，登录后附带本人点赞/归属信息。 */
    // 接口：GET /api/app/comments；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestParam：读取URL查询参数；required=false允许省略，defaultValue提供缺省值。
    @GetMapping("/comments")
    public Result<Page<CommentRespDto>> comments(@RequestParam Long bookId,@RequestParam(required=false) Long chapterId,
        @RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize,HttpServletRequest request){
        return Result.ok(
                service.comments(bookId,chapterId,auth.resolveOptionalUserId(request),pageNum,pageSize)
        );
    }

    /** 发表书评或章评；参数先校验，再限流，再交给Service检查内容与写入。 */
    // 接口：POST /api/app/interaction/comments；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestBody：将请求体JSON转换为DTO；@Valid触发DTO上的非空、长度、金额范围等校验。
    @PostMapping("/interaction/comments")
    public Result<Long> comment(@Valid @RequestBody CommentReqDto req){
        limit();
        return Result.ok(service.addComment(UserContext.getUserId(),req));
    }

    /** 删除评论；Service还要验证该评论确实属于当前用户。 */
    // 接口：DELETE /api/app/interaction/comments/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @DeleteMapping("/interaction/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id){
        service.deleteComment(UserContext.getUserId(),id);
        return Result.ok();
    }

    /** 点赞，返回最新点赞数；重复请求不会重复加一。 */
    // 接口：POST /api/app/interaction/comments/{id}/like；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @PostMapping("/interaction/comments/{id}/like")
    public Result<Integer> like(@PathVariable Long id){
        return Result.ok(service.like(UserContext.getUserId(),id,true));
    }

    /** 取消点赞；已取消的重复请求也可安全处理。 */
    // 接口：DELETE /api/app/interaction/comments/{id}/like；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @DeleteMapping("/interaction/comments/{id}/like")
    public Result<Integer> unlike(@PathVariable Long id){
        return Result.ok(service.like(UserContext.getUserId(),id,false));
    }

    /** 社区广场分页：只返回公开可见帖子。 */
    // 接口：GET /api/app/forum/posts；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestParam：读取URL查询参数；required=false允许省略，defaultValue提供缺省值。
    @GetMapping("/forum/posts")
    public Result<Page<ForumPostRespDto>> posts(@RequestParam(defaultValue="1") int pageNum,
        @RequestParam(defaultValue="20") int pageSize,HttpServletRequest request){
        return Result.ok(service.posts(auth.resolveOptionalUserId(request),false,pageNum,pageSize));
    }

    /** 本人帖子分页：包括待审核、已通过、驳回及封帖状态。 */
    // 接口：GET /api/app/interaction/forum/mine；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestParam：读取URL查询参数；required=false允许省略，defaultValue提供缺省值。
    @GetMapping("/interaction/forum/mine")
    public Result<Page<ForumPostRespDto>> mine(@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize){
        return Result.ok(service.posts(UserContext.getUserId(),true,pageNum,pageSize));
    }

    /** 按帖子ID查详情；Service根据可选登录身份判断是否有权查看。 */
    // 接口：GET /api/app/forum/posts/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @GetMapping("/forum/posts/{id}")
    public Result<ForumPostRespDto> post(@PathVariable Long id,HttpServletRequest request){
        return Result.ok(service.post(id,auth.resolveOptionalUserId(request)));
    }

    /** 发布新帖子，默认进入待审核队列。 */
    // 接口：POST /api/app/interaction/forum/posts；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestBody：将请求体JSON转换为DTO；@Valid触发DTO上的非空、长度、金额范围等校验。
    @PostMapping("/interaction/forum/posts")
    public Result<Long> addPost(@Valid @RequestBody ForumPostReqDto req){
        limit();
        return Result.ok(service.addPost(UserContext.getUserId(),req));
    }

    /** 删除本人帖子，执行逻辑删除。 */
    // 接口：DELETE /api/app/interaction/forum/posts/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @DeleteMapping("/interaction/forum/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id){
        service.deletePost(UserContext.getUserId(),id);
        return Result.ok();
    }

    /** 先验证帖子可见，再查公开回复及查看者自己的回复。 */
    // 接口：GET /api/app/forum/posts/{id}/replies；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    // @RequestParam：读取URL查询参数；required=false允许省略，defaultValue提供缺省值。
    @GetMapping("/forum/posts/{id}/replies")
    public Result<Page<ForumReplyRespDto>> replies(@PathVariable Long id,@RequestParam(defaultValue="1") int pageNum,
        @RequestParam(defaultValue="20") int pageSize,HttpServletRequest request){
        return Result.ok(service.replies(id,auth.resolveOptionalUserId(request),pageNum,pageSize));
    }

    /** 对公开且未封帖的帖子新增回复，回复本身仍需审核。 */
    // 接口：POST /api/app/interaction/forum/posts/{id}/replies；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    // @RequestBody：将请求体JSON转换为DTO；@Valid触发DTO上的非空、长度、金额范围等校验。
    @PostMapping("/interaction/forum/posts/{id}/replies")
    public Result<Long> reply(@PathVariable Long id,@Valid @RequestBody ForumReplyReqDto req){
        limit();
        return Result.ok(service.addReply(UserContext.getUserId(),id,req));}

    /** 删除本人回复，不能通过传入他人的回复ID越权删除。 */
    // 接口：DELETE /api/app/interaction/forum/replies/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @DeleteMapping("/interaction/forum/replies/{id}")
    public Result<Void> deleteReply(@PathVariable Long id){service.deleteReply(UserContext.getUserId(),id);return Result.ok();}

    // 接口：POST /api/app/interaction/forum/posts/{id}/like；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @PostMapping("/interaction/forum/posts/{id}/like")
    public Result<Long> likePost(@PathVariable Long id){return Result.ok(service.likePost(UserContext.getUserId(),id,true));}
    // 接口：DELETE /api/app/interaction/forum/posts/{id}/like；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @DeleteMapping("/interaction/forum/posts/{id}/like")
    public Result<Long> unlikePost(@PathVariable Long id){return Result.ok(service.likePost(UserContext.getUserId(),id,false));}
    // 接口：POST /api/app/interaction/forum/replies/{id}/like；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @PostMapping("/interaction/forum/replies/{id}/like")
    public Result<Long> likeReply(@PathVariable Long id){return Result.ok(service.likeReply(UserContext.getUserId(),id,true));}
    // 接口：DELETE /api/app/interaction/forum/replies/{id}/like；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @DeleteMapping("/interaction/forum/replies/{id}/like")
    public Result<Long> unlikeReply(@PathVariable Long id){return Result.ok(service.likeReply(UserContext.getUserId(),id,false));}

    /** 评论、帖子、回复共用每用户每分钟10次发布额度，防止高频灌水。 */
    private void limit(){rate.rateLimit("publish:"+UserContext.getUserId(),10,60);}
}
