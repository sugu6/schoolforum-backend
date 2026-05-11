package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.Categories;

/**
 * 分类表 映射层。
 *
 * @author sugu
 * @since 2026-03-08
 */
@Mapper
public interface CategoriesMapper extends BaseMapper<Categories> {

}
