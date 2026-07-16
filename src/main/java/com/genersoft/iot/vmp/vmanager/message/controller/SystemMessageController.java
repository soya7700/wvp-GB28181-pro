package com.genersoft.iot.vmp.vmanager.message.controller;

import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.vmanager.message.bean.SystemMessage;
import com.genersoft.iot.vmp.vmanager.message.service.ISystemMessageService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "站内消息")
@RestController
@RequestMapping("/api/message")
public class SystemMessageController {
    @Autowired
    private ISystemMessageService service;

    @GetMapping("/list")
    @Operation(summary = "当前用户站内消息")
    public PageInfo<SystemMessage> list(@RequestParam int page, @RequestParam int count,
                                        @RequestParam(required = false) Boolean readFlag,
                                        @RequestParam(required = false) String type) {
        return service.query(SecurityUtils.getUserId(), page, count, readFlag, type);
    }

    @GetMapping("/{id}")
    public SystemMessage detail(@PathVariable Integer id) {
        service.markRead(id, SecurityUtils.getUserId());
        return service.getOne(id, SecurityUtils.getUserId());
    }

    @GetMapping("/unread/count")
    public int unreadCount() {
        return service.unreadCount(SecurityUtils.getUserId());
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Integer id) {
        service.markRead(id, SecurityUtils.getUserId());
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        service.markAllRead(SecurityUtils.getUserId());
    }
}
