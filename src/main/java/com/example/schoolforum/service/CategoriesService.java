package com.example.schoolforum.service;

import com.example.schoolforum.enums.ActiveStatus;
import com.mybatisflex.core.service.IService;
import com.example.schoolforum.pojo.Categories;

import java.util.List;

/**
 * 分类表 服务层。
 *
 * @author sugu
 * @since 2026-03-08
 */
public interface CategoriesService extends IService<Categories> {

    List<Categories> listAll();

    List<Categories> listEnabled();

    List<Categories> getChildrenByParentId(Long parentId);

    List<Categories> listByLevel(Integer level);

    Categories createCategory(String name, Long parentId, Integer level);

    Categories updateCategory(Long id, String name, ActiveStatus status);

    void deleteCategory(Long id);

    void updatePostCount(Long categoryId);

    List<Long> getCategoryAndChildrenIds(Long categoryId);
}