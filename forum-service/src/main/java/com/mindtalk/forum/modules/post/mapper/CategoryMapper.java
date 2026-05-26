package com.mindtalk.forum.modules.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.post.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
