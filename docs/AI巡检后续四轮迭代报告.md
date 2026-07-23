# AI 巡检后续四轮迭代报告

分支：`agent/wvp-mobile-station-messages`

## 迭代一：调度与安全回调

- 自动扫描到期计划并创建巡检任务。
- 配置 AI 服务后自动派发任务，失败写入任务状态。
- AI 结果和完成回调采用 HMAC-SHA256 签名、时间窗校验和 Redis 随机数防重放。
- 多实例调度使用 Redis 分布式锁，AI 派发失败按配置自动重试。
- `callbackId` 唯一约束保证重复回调幂等。
- 数据库脚本：`AI巡检-迭代1-mysql.sql`。

## 迭代二：巡检范围与时间策略

- 计划支持新增、编辑、复制、启停和删除。
- 支持通道集合、检测类型、星期和执行时间窗。
- 自动调度只在计划允许的日期和时间范围内执行。
- 移动端提供轻量计划维护表单。
- 数据库脚本：`AI巡检-迭代2-mysql.sql`。

## 迭代三：异常处置工作台

- 异常支持优先级、负责人、认领和处理状态。
- 数据库条件更新保护并发认领。
- 五分钟内同通道同类型未关闭异常自动合并并累计次数。
- 移动端展示负责人、优先级、出现次数和处置状态。
- 数据库脚本：`AI巡检-迭代3-mysql.sql`。

## 迭代四：统计与运维

- 增加近 7～90 日趋势和高频异常通道统计。
- 增加数据库迁移、AI 服务配置健康检查。
- 启动时检查 AI 巡检表结构并输出明确提示。
- 管理员可清理带 `CODEX-TEST-` 前缀的测试数据及其关联告警、站内信。
- 增加本地环境模板和 AI 配置示例，凭据通过环境变量注入。
- Kingbase 驱动按 CPU 架构显式选择，避免同时打包两个架构驱动。

## 数据库执行顺序

新环境直接执行最新版 `AI巡检-mysql.sql`。

已经完成基础迁移的环境按顺序执行：

1. `AI巡检-迭代1-mysql.sql`
2. `AI巡检-迭代2-mysql.sql`
3. `AI巡检-迭代3-mysql.sql`

PostgreSQL/Kingbase 使用同名的 `postgresql-kingbase` 基础脚本或三份增量脚本。
当前环境未提供 PostgreSQL/Kingbase 实例，因此完成了脚本静态核对和 Java 侧调度
SQL 去方言化，数据库实机执行仍需在对应测试环境验证。

## AI 回调签名

启用 `callback-token` 后，AI 服务回调需携带：

- `X-AI-Timestamp`：Unix 秒时间戳。
- `X-AI-Nonce`：每次请求唯一的随机字符串。
- `X-AI-Signature`：HMAC-SHA256 小写十六进制签名。

签名原文依次为 `timestamp`、`nonce`、`taskId`、`action`、`callbackId`，
字段之间使用换行符连接。结果回调的 `action` 为 `result`，完成回调为
`complete` 且 `callbackId` 为空。

## 构建与测试

- Java 8 后端自动化测试：24 个测试全部通过。
- Maven `test package`：通过。
- UniApp TypeScript 检查：通过。
- H5 构建：通过。
- 微信小程序构建：通过。

Kingbase 构建参数：

- ARM64：`mvn package -Dkingbase.aarch64`
- x86_64：`mvn package -Dkingbase.x86_64`
