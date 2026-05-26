package com.mindtalk.forum.modules.post.service;

import com.mindtalk.forum.modules.post.dto.CreateTagDTO;
import com.mindtalk.forum.modules.post.vo.TagVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {

    /** 标签列表 */
    List<TagVO> list();

    /** 新增标签 */
    TagVO create(CreateTagDTO dto);

    /** 编辑标签 */
    TagVO update(Long id, CreateTagDTO dto);

    /** 删除标签 */
    void delete(Long id);
}
