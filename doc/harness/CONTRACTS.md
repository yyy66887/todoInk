# TodoInk 业务契约

以下是后续实现与验收使用的目标，不代表当前代码全部符合。实际差距以 [PROJECT](PROJECT.md) 为准，验证编号见 [VERIFICATION](VERIFICATION.md)。

## 唯一事实来源

字段与类型以 [Draft](../../app/src/main/java/com/todoink/app/notification/model/NotificationSnapshotDraft.kt)、[ParsedMessage](../../app/src/main/java/com/todoink/app/notification/model/ParsedMessage.kt)、[Parser 接口](../../app/src/main/java/com/todoink/app/notification/parser/NotificationParser.kt)、[Entity](../../app/src/main/java/com/todoink/app/data/db/NotificationSnapshotEntity.kt) 为准；持久化结构以 Room Entity、数据库版本与导出 schema 一同核对。文档不另建第二份手工维护的 DTO。

## Phase 1 不变量

| ID | 可观察契约 | 主要位置 | 场景 |
| --- | --- | --- | --- |
| C01 | 未选来源在读取正文前拒绝；设置未加载时默认拒绝；实时与补采一致 | Service / Settings / Recorder | V01、V02 |
| C02 | 来源关闭保存完成后，新回调及尚未开始持久化的排队项不得保存；已提交历史数据不自动删除 | 设置更新与消费侧检查 | V02 |
| C03 | 回调只做允许检查、轻量复制与入队；异步处理不持有 Notification / Bundle | Extractor / Draft | V01、V09 |
| C04 | 同一 key 内容语义相同只增加 observeCount；postTime / receivedAt 单独变化不产生新内容版本；有效正文变化保留递增版本 | Repository / DAO | V03、V04 |
| C05 | 不同 key 相同文字不强行合并；快照去重不等于消息或任务去重；A→B→A 是三个连续版本 | Repository | V04 |
| C06 | 优先 MessagingStyle，再 textLines、bigText、text；过滤空白，不机械拼接重复正文；解析可返回 0..n 条消息 | Parser | V05 |
| C07 | 可用性标签只描述观察到的可用内容，不保证整段聊天完整；未知发送人 / 会话保持未知；汇总通知与群组成员区分 | Extractor / Parser | V06 |
| C08 | 解析不支持可保留受控诊断快照；落库失败、队列丢弃与成功分别记录；取消继续传播，不能伪装成 save_failed | Recorder / Status | V07、V09 |
| C09 | 访问授权、监听连接、最近回调、最近保存分别表达；重连只补采当前活跃通知，不声称补全历史 | Status / Service | V08 |
| C10 | 关闭重开后仍能查看保留期内的已保存正文与原始受控字段；倒序使用 receivedAt，字段含义不混淆 | Room / Inbox / Detail | V10 |
| C11 | 默认本地保存，默认保留 3 天；正文不进入普通日志 / 测试报告；数据库与设置不经自动备份泄漏 | Settings / 存储 / 备份配置 | V11、V12 |
| C12 | schema 变化同时更新数据库版本、迁移与导出结构；升级不得静默清空通知 | Room | V13 |

C02 的竞态边界须在 NI-001 明确：以来源关闭操作返回成功为边界；已开始提交的事务可完成，尚未开始的处理必须读取更新后的设置。避免用异步缓存传播的不确定时延代替此保证。

C04 的哈希输入必须显式设计：包括实际保存的正文结构、必要的来源 / 可用性语义；采用不产生字段拼接歧义的编码，明确定义 null、空值、顺序与时间。不能删掉 postTime 后就假定所有去重问题已解决。

## 错误与恢复

当前 `Outcome` 只有 SAVED / DUPLICATE_CONTENT，状态错误为字符串；以下是目标语义，不要求立刻引入通用 ErrorEnvelope 框架。

| 情况 | 数据结果 | 对外行为 / 恢复 |
| --- | --- | --- |
| 来源未允许 | 未开始保存 | 正常拒绝，不记录正文，不报数据库错误 |
| 相同内容重放 | 计数更新 | 不新增内容版本 |
| 解析不支持 | 原始受控字段仍可保存 | 标记 unsupported，不冒充成功解析 |
| 队列丢弃 | 被丢项未保存 | 可观察的丢弃计数 / 状态；不宣称无丢失 |
| 保存失败 | 事务失败或结果待核对 | 脱敏错误码；确认数据库结果后再决定重试 |
| 协程取消 | 是否提交需按实际状态判断 | 传播取消，不自动重放、不报告成功 |

重试只允许有次数上限且能证明幂等的操作。解析器异常降级与数据库操作重放属于不同策略。

## 后续阶段预留

- Phase 2：本地或云端 AI 的有效结果只创建 CANDIDATE，用户确认才进入 CONFIRMED；任务状态与通知快照状态分开。消息身份、时间精度、幂等处理、用户编辑保护和来源清理见 [Phase 2 方案](MVP_PHASE2_PLAN.md)，仍是待实施契约。
- AI 模块：遵守 [AI-C01–AI-C08](../product/AI/README.md)；统一校验结构、来源证据与有效版本，本地不自动转云端，云端来源单独允许，迟到结果不能覆盖用户编辑。通知正文只作为数据，不成为工具执行指令。Phase 3 仅保留后续质量与效率优化，不再作为首次 AI 接入阶段。
- Phase 4–5：Android 负责渲染，ESP32 接收位图。当前 [DeviceTransport](../../app/src/main/java/com/todoink/app/device/DeviceTransport.kt) 注释要求 DISPLAYED 才算成功；PRD 的 ACK / CRC_OK 尚不足以证明显示完成，协议实施时需明确版本、帧 ID、校验、超时、重传和最终确认。

这些预留是未来任务的输入，本次不据此添加 TODO、网络权限、AI SDK、BLE 库或固件。
