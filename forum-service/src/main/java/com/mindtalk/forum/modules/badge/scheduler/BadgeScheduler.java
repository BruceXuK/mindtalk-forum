package com.mindtalk.forum.modules.badge.scheduler;

import com.mindtalk.forum.modules.badge.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeScheduler {

    private final BadgeService badgeService;

    @Scheduled(cron = "${scheduling.badge-eval-cron:0 0 */6 * * *}")
    public void evaluateAllUsers() {
        log.info("[勋章评估] 开始全量用户勋章评估");
        badgeService.evaluateAllUsers();
        log.info("[勋章评估] 完成");
    }
}
