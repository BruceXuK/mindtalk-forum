package com.mindtalk.forum.modules.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.post.dto.CreateCategoryDTO;
import com.mindtalk.forum.modules.post.entity.Category;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.CategoryMapper;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.post.service.CategoryService;
import com.mindtalk.forum.modules.post.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分类服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final PostMapper postMapper;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY = Constants.REDIS_PREFIX + "category:list";
    private static final long CACHE_TTL = 30;

    @Override
    public List<CategoryVO> list() {
        String cached = redisUtils.get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<CategoryVO>>() {});
            } catch (JsonProcessingException e) {
                log.debug("[缓存] 分类列表反序列化失败");
            }
        }

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder);
        List<Category> categories = categoryMapper.selectList(wrapper);

        List<CategoryVO> vos = categories.stream().map(c -> CategoryVO.builder()
                .id(c.getId()).name(c.getName()).description(c.getDescription())
                .icon(c.getIcon()).sortOrder(c.getSortOrder()).postCount(c.getPostCount())
                .build()).toList();

        try {
            redisUtils.set(CACHE_KEY, objectMapper.writeValueAsString(vos),
                    CACHE_TTL, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("[缓存] 分类列表序列化失败");
        }
        return vos;
    }

    @Override
    @Transactional
    public CategoryVO create(CreateCategoryDTO dto) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, dto.getName());
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw BusinessException.conflict("分类名称已存在");
        }

        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .icon(dto.getIcon())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .postCount(0)
                .status(1)
                .build();
        categoryMapper.insert(category);

        redisUtils.delete(CACHE_KEY);
        log.info("[分类] 创建成功 id={} name={}", category.getId(), category.getName());
        return CategoryVO.builder()
                .id(category.getId()).name(category.getName())
                .description(category.getDescription()).icon(category.getIcon())
                .sortOrder(category.getSortOrder()).postCount(0).status(category.getStatus()).build();
    }

    @Override
    @Transactional
    public CategoryVO update(Long id, CreateCategoryDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }

        if (dto.getName() != null) category.setName(dto.getName());
        if (dto.getDescription() != null) category.setDescription(dto.getDescription());
        if (dto.getIcon() != null) category.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) category.setSortOrder(dto.getSortOrder());
        categoryMapper.updateById(category);

        redisUtils.delete(CACHE_KEY);
        log.info("[分类] 编辑成功 id={}", id);
        return CategoryVO.builder()
                .id(category.getId()).name(category.getName())
                .description(category.getDescription()).icon(category.getIcon())
                .sortOrder(category.getSortOrder()).postCount(category.getPostCount()).status(category.getStatus()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }
        LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(Post::getCategoryId, id);
        long postCount = postMapper.selectCount(postWrapper);
        if (postCount > 0) {
            throw BusinessException.conflict("该分类下有 " + postCount + " 篇帖子，请先转移后再删除");
        }
        categoryMapper.deleteById(id);
        redisUtils.delete(CACHE_KEY);
        log.info("[分类] 删除成功 id={}", id);
    }

    @Override
    public List<CategoryVO> listAll() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        return categoryMapper.selectList(wrapper).stream()
                .map(c -> CategoryVO.builder()
                        .id(c.getId()).name(c.getName()).description(c.getDescription())
                        .icon(c.getIcon()).sortOrder(c.getSortOrder()).postCount(c.getPostCount())
                        .status(c.getStatus())
                        .build()).toList();
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }
        category.setStatus(category.getStatus() == 1 ? 0 : 1);
        categoryMapper.updateById(category);
        redisUtils.delete(CACHE_KEY);
        log.info("[分类] 状态切换 id={} status={}", id, category.getStatus());
    }

    @Override
    @Transactional
    public void batchSort(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            Category category = new Category();
            category.setId(id);
            category.setSortOrder(sortOrder);
            categoryMapper.updateById(category);
        }
        redisUtils.delete(CACHE_KEY);
        log.info("[分类] 批量排序完成 count={}", items.size());
    }
}
