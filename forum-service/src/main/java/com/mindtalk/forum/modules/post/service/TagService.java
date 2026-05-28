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

    /** 管理员：全部标签列表（含禁用的） */
    List<TagVO> listAll();

    /** 新增标签 */
    TagVO create(CreateTagDTO dto);

    /** 编辑标签 */
    TagVO update(Long id, CreateTagDTO dto);

    /** 删除标签（有关联帖子时抛异常） */
    void delete(Long id);

    /** 切换启用/禁用状态 */
    void toggleStatus(Long id);
}
