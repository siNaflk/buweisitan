package com.xunye.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xunye.common.Result;
import com.xunye.dao.BookCategoryMapper;
import com.xunye.dto.resp.CategoryRespDto;
import com.xunye.entity.BookCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口（App 端）
 */
// @RestController：注册为Spring控制器，方法返回值由框架序列化成JSON响应。
@RestController
// @RequestMapping：设置这个控制器的公共路径前缀；完整接口路径还要拼接方法上的映射。
@RequestMapping("/api/app/category")
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class CategoryController {

    private final BookCategoryMapper categoryMapper;

    /** 启用中的分类列表（按 sort 排序） */
    // 接口：GET /api/app/category/list；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/list")
    public Result<List<CategoryRespDto>> list() {
        List<BookCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<BookCategory>()
                .eq(BookCategory::getStatus, 0)
                .orderByAsc(BookCategory::getSort));
        return Result.ok(categories.stream().map(category -> {
            CategoryRespDto dto = new CategoryRespDto();
            BeanUtils.copyProperties(category, dto);
            return dto;
        }).toList());
    }
}
