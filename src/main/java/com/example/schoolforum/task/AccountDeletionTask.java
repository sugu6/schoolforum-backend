package com.example.schoolforum.task;

import com.example.schoolforum.service.AccountDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountDeletionTask {

    private final AccountDeletionService accountDeletionService;

    @Scheduled(cron = "0 0 * * * ?")
    public void processExpiredDeletions() {
        log.info("开始处理过期账户注销申请");
        try {
            int processedCount = accountDeletionService.processExpiredDeletionRequests();
            log.info("账户注销处理完成: 共处理{}个申请", processedCount);
        } catch (Exception e) {
            log.error("账户注销处理失败", e);
        }
    }
}