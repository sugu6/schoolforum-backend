package com.example.schoolforum.service.impl;

import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.CategoriesMapper;
import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.pojo.Categories;
import com.example.schoolforum.service.CategoriesService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.schoolforum.pojo.table.CategoriesTableDef.CATEGORIES;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriesServiceImpl extends ServiceImpl<CategoriesMapper, Categories> implements CategoriesService {

    private final CategoriesMapper categoriesMapper;
    private final PostsMapper postsMapper;

    @Override
    public List<Categories> listAll() {
        QueryWrapper wrapper = QueryWrapper.create()
                .orderBy(CATEGORIES.LEVEL, true)
                .orderBy(CATEGORIES.ID, true);
        return this.list(wrapper);
    }

    @Override
    public List<Categories> listEnabled() {
        List<Categories> allCategories = this.list(QueryWrapper.create()
                .where(CATEGORIES.STATUS.eq(ActiveStatus.ACTIVE))
                .orderBy(CATEGORIES.LEVEL, true)
                .orderBy(CATEGORIES.ID, true));

        Map<Long, List<Categories>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Categories::getParentId));

        List<Categories> rootCategories = allCategories.stream()
                .filter(c -> c.getLevel() == 1)
                .collect(Collectors.toList());

        for (Categories root : rootCategories) {
            List<Categories> children = childrenMap.get(root.getId());
            root.setChildren(children != null ? children : new ArrayList<>());
        }

        return rootCategories;
    }

    @Override
    public List<Categories> getChildrenByParentId(Long parentId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CATEGORIES.PARENT_ID.eq(parentId))
                .where(CATEGORIES.STATUS.eq(ActiveStatus.ACTIVE))
                .orderBy(CATEGORIES.ID, true);
        return this.list(wrapper);
    }

    @Override
    public List<Categories> listByLevel(Integer level) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CATEGORIES.LEVEL.eq(level))
                .where(CATEGORIES.STATUS.eq(ActiveStatus.ACTIVE))
                .orderBy(CATEGORIES.ID, true);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public Categories createCategory(String name, Long parentId, Integer level) {
        if (getByName(name) != null) {
            throw new BusinessException("分类名称已存在");
        }

        Categories category = new Categories();
        category.setName(name);
        category.setParentId(parentId);
        category.setLevel(level != null ? level : 1);
        category.setStatus(ActiveStatus.ACTIVE);
        category.setPostCount(0);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        this.save(category);
        log.info("分类创建成功: id={}, name={}", category.getId(), name);
        return category;
    }

    @Override
    @Transactional
    public Categories updateCategory(Long id, String name, ActiveStatus status) {
        Categories category = this.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        if (name != null && !name.equals(category.getName())) {
            if (getByName(name) != null) {
                throw new BusinessException("分类名称已存在");
            }
            category.setName(name);
        }
        if (status != null) {
            category.setStatus(status);
        }
        category.setUpdatedAt(LocalDateTime.now());

        this.updateById(category);
        log.info("分类更新成功: id={}", id);
        return category;
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Categories category = this.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        this.removeById(id);
        log.info("分类删除成功: id={}, name={}", id, category.getName());
    }

    @Override
    @Transactional
    public void updatePostCount(Long categoryId) {
        if (categoryId == null) {
            return;
        }

        QueryWrapper countWrapper = QueryWrapper.create()
                .where("category_id = ?", categoryId);

        long count = postsMapper.selectCountByQuery(countWrapper);

        Categories category = UpdateEntity.of(Categories.class, categoryId);
        category.setPostCount((int) count);
        category.setUpdatedAt(LocalDateTime.now());
        categoriesMapper.update(category);
    }

    @Override
    public List<Long> getCategoryAndChildrenIds(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        
        List<Categories> children = getChildrenByParentId(categoryId);
        for (Categories child : children) {
            ids.add(child.getId());
        }
        
        return ids;
    }

    private Categories getByName(String name) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CATEGORIES.NAME.eq(name));
        return this.getOne(wrapper);
    }
}
