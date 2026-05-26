package com.mindtalk.forum.modules.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsOverviewVO {

    private Long totalUsers;

    private Long totalPosts;

    private Long totalComments;

    private Long totalReports;

    private Long todayNewUsers;

    private Long todayNewPosts;

    private Long todayNewComments;

    private Long pendingReports;
}
