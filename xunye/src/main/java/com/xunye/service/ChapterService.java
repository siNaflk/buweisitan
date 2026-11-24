package com.xunye.service;

import com.xunye.dto.resp.ChapterContentRespDto;

/**
 * 章节服务：章节内容读取与阅读权限判定
 */
public interface ChapterService {

    /**
     * 读取章节内容（含权限判定）
     *
     * @param chapterId 章节 ID
     * @param userId    当前用户 ID，游客为 null
     */
    ChapterContentRespDto content(Long chapterId, Long userId);
}
