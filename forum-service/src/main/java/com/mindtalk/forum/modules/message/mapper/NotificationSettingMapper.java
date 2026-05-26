package com.mindtalk.forum.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.message.entity.NotificationSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationSettingMapper extends BaseMapper<NotificationSetting> {

    List<NotificationSetting> selectByUserId(@Param("userId") Long userId);

    boolean isEnabled(@Param("userId") Long userId, @Param("notifyType") String notifyType);
}
