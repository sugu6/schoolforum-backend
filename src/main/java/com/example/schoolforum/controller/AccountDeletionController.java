package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.enums.DeletionStatus;
import com.example.schoolforum.pojo.vo.AccountDeletionRequestVO;
import com.example.schoolforum.service.AccountDeletionService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account-deletion")
@RequiredArgsConstructor
@Tag(name = "账户注销管理", description = "账户注销申请管理接口，管理员和超管可查看注销申请列表")
public class AccountDeletionController {

    private final AccountDeletionService accountDeletionService;

    @GetMapping("list/page")
    @Operation(summary = "分页查询注销申请", description = "管理员权限，分页获取账户注销申请列表，可按状态筛选")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    public Page<AccountDeletionRequestVO> listPage(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") Integer pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态筛选：PENDING-待处理，CANCELLED-已撤销，COMPLETED-已完成") @RequestParam(required = false) DeletionStatus status) {
        return accountDeletionService.listPage(pageNumber, pageSize, status);
    }
}
