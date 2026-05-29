package com.mindtalk.forum.modules.post.service;

import com.mindtalk.forum.modules.post.dto.CreateTagDTO;
import com.mindtalk.forum.modules.post.vo.TagVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {

    /** 标签列表（仅启用的，前台用） */
    List<TagVO> list();

    /** 按名称模糊搜索标签（仅启用的），用于 autocomplete */
    List<TagVO> search(String q);

    /** 根据名称查找或创建标签（用户发帖时自动创建） */
    TagVO getOrCreate(String name, String description);

    /** 管理员：全部标签列表（含禁用的） */
    List<TagVO> listAll();

    /** 新增标签 */
    TagVO create(CreateTagDTO dto);

    /** 编辑标签 */
    TagVO update(Long id, CreateTagDTO dto);

    /** 删除标签（自动解除关联） */
    void delete(Long id);

    /** 合并标签：将 sourceIds 的所有帖子关联迁移到 targetId，删除源标签 */
    void merge(List<Long> sourceIds, Long targetId);

    /** 切换启用/禁用状态 */
    void toggleStatus(Long id);
}
