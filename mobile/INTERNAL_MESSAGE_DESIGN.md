# 站内消息与告警闭环

## 当前实现

1. WVP 收到设备告警并写入 `wvp_device_alarm`。
2. 系统为所有现有用户生成一条 `wvp_system_message` 收件箱消息。
3. 每位用户拥有独立的未读/已读状态。
4. 点击消息进入关联告警详情，同时将当前用户消息标为已读。
5. 用户可以确认告警、标记误报或完成处置，并填写备注。
6. 告警处置状态对所有用户可见，避免重复处置。

状态流转：

```text
PENDING（待处理） → ACKNOWLEDGED（已确认） → RESOLVED（已处理）
        └────────────────────────────→ FALSE_ALARM（误报）
```

## API

- `GET /api/message/list`：当前用户消息分页列表。
- `GET /api/message/unread/count`：未读数。
- `POST /api/message/{id}/read`：单条已读。
- `POST /api/message/read-all`：全部已读。
- `GET /api/alarm/{id}`：告警详情。
- `POST /api/alarm/{id}/handle`：告警处置。

## 后续扩展

消息表的 `type`、`business_type` 和 `business_id` 已保留通用关联能力，后续可增加：

- `SYSTEM`：版本、配置和系统公告。
- `DEVICE_OFFLINE`：设备持续离线通知。
- `MEDIA_SERVER`：媒体节点异常和恢复。
- `STREAM_FAILURE`：推拉流连续失败。
- `TASK`：录像下载、导入和同步任务完成通知。

下一阶段建议增加通知规则，按用户选择等级、设备、业务分组和免打扰时间；仍只写站内收件箱，不对接微信或 App 推送。
