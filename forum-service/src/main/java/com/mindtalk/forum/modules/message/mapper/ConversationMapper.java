package com.mindtalk.forum.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.message.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    List<Conversation> selectByUserId(@Param("userId") Long userId);

    Conversation findByUserPair(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
