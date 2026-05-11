package com.example.schoolforum.service;

import com.example.schoolforum.enums.DeletionStatus;
import com.example.schoolforum.pojo.AccountDeletionRequest;
import com.example.schoolforum.pojo.vo.AccountDeletionRequestVO;
import com.mybatisflex.core.paginate.Page;

public interface AccountDeletionService {

    AccountDeletionRequest requestDeletion(Long userId, String reason);

    void cancelDeletion(Long userId);

    AccountDeletionRequest getPendingRequest(Long userId);

    int processExpiredDeletionRequests();

    Page<AccountDeletionRequestVO> listPage(Integer pageNumber, Integer pageSize, DeletionStatus status);
}