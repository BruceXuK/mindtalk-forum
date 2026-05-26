package com.mindtalk.forum.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.forum.modules.admin.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    IPage<Report> selectPageWithReporter(Page<Report> page,
                                         @Param("status") Integer status,
                                         @Param("targetType") String targetType);
}
