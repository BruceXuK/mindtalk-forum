package com.mindtalk.forum.modules.post.service;

import com.mindtalk.forum.modules.post.dto.CreateCategoryDTO;
import com.mindtalk.forum.modules.post.vo.CategoryVO;

import java.util.List;
import java.util.Map;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /** 分类列表（仅启用的，前台用） */
    List<CategoryVO> list();

    /** 管理员：全部分类列表（含禁用的） */
    List<CategoryVO> listAll();

    /** 新增分类 */
    CategoryVO create(CreateCategoryDTO dto);

    /** 编辑分类 */
    CategoryVO update(Long id, CreateCategoryDTO dto);

    /** 删除分类（有帖子关联时抛异常） */
    void delete(Long id);

    /** 切换启用/禁用状态 */
    void toggleStatus(Long id);

    /** 批量更新排序 */
    void batchSort(List<Map<String, Object>> items);
}
