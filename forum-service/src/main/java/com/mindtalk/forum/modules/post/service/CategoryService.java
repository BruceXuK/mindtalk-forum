package com.mindtalk.forum.modules.post.service;

import com.mindtalk.forum.modules.post.dto.CreateCategoryDTO;
import com.mindtalk.forum.modules.post.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /** 分类列表 */
    List<CategoryVO> list();

    /** 新增分类 */
    CategoryVO create(CreateCategoryDTO dto);

    /** 编辑分类 */
    CategoryVO update(Long id, CreateCategoryDTO dto);

    /** 删除分类 */
    void delete(Long id);
}
