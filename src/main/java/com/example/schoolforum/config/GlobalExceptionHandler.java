package com.example.schoolforum.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.pojo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {}", e.getMessage());
        if (isSseRequest(request)) {
            log.warn("SSE 请求中发生业务异常，无法返回错误响应");
            return null;
        }
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        if (isSseRequest(request)) {
            log.warn("SSE 请求中 Sa-Token 上下文已失效: {}", e.getMessage());
            return null;
        }
        
        String message;
        String type = e.getType();
        
        if (NotLoginException.NOT_TOKEN.equals(type)) {
            message = "未提供登录凭证";
        } else if (NotLoginException.INVALID_TOKEN.equals(type)) {
            message = "登录凭证无效";
        } else if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            message = "登录已过期，请重新登录";
        } else if (NotLoginException.BE_REPLACED.equals(type)) {
            message = "账号已在其他设备登录";
        } else if (NotLoginException.KICK_OUT.equals(type)) {
            message = "账号已被踢下线";
        } else if (NotLoginException.TOKEN_FREEZE.equals(type)) {
            message = "账号已被冻结";
        } else if (NotLoginException.NO_PREFIX.equals(type)) {
            message = "登录凭证格式错误";
        } else {
            message = "请先登录";
        }
        
        log.warn("未登录异常: {}, 类型: {}", e.getMessage(), type);
        return Result.error(message);
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        String permission = e.getPermission();
        log.warn("权限不足异常: 缺少权限 [{}]", permission);
        if (isSseRequest(request)) {
            return null;
        }
        return Result.error("权限不足，缺少权限: " + permission);
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        String role = e.getRole();
        log.warn("角色不足异常: 缺少角色 [{}]", role);
        if (isSseRequest(request)) {
            return null;
        }
        return Result.error("角色权限不足，缺少角色: " + role);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {}", e.getMessage(), e);
        if (isSseRequest(request)) {
            log.warn("SSE 请求中发生系统异常，无法返回错误响应");
            return null;
        }
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = "系统繁忙，请稍后重试";
        }
        return Result.error(message);
    }

    private boolean isSseRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        return (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE))
                || (contentType != null && contentType.contains(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
}
