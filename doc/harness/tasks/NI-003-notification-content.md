# NI-003：完整复制、解析并保存通知内容

- 状态：READY → IN_PROGRESS（2026-09-26 产品 MVP v0.1 获批后按 MVP-M0 顺序开工）。
- 里程碑：M2；优先级：P0；依赖：NI-000、NI-001。
- 契约：C03、C06、C07、C10、C12；场景：V05、V06、V10 的存储部分、V13。

## 当前问题与目标

GenericParser 的降级正文未转换为 messages；Extractor 的分组与汇总标记需要区分；MessagingStyle 只保存解析条数，不能在重开后恢复原消息。目标是建立可追溯、可保存的受控快照与 0..n 消息解析结果。

## 两步交付

### A：字段复制与解析

- 核对系统字段语义，区分汇总通知、普通群组成员、隐藏 / 空正文；复制轻量值，不把 Bundle 留给后台协程。
- 明确 MessagingStyle → textLines → bigText → text 的选择规则，过滤空白、保留顺序、避免重复拼接。
- 缺发送人 / 会话信息保持未知；summary 可用于诊断，不视为独立真实消息来生成后续业务。
- 根据实际来源的脱敏样本决定是否增加专属 Parser，并在 Registry 注册；不凭名称假设字段格式。

### B：受控结构持久化

- 保存重现 Raw / Normalized 详情所需的消息结构、解析版本与状态；沿用快照层，不建立独立 ObservedMessage 或 Todo 表。
- 明确编码和 null / 空列表语义；包含换行、U+001F、中文、空元素的受控字段应无损往返。
- 如需 schema 变化，递增版本、保留旧 schema、提供迁移。历史未保存的正文标记不足，不补造数据或清空旧库。
- 向 NI-002 给出最终正文 / 语义字段清单，作为内容哈希设计输入；读取模型由 NI-006 消费。

范围：SnapshotExtractor、notification/model、parser、Entity / Converters / Room migration、Repository 的字段映射。

## 验收

| 场景 | 预期结果 | 状态 |
| --- | --- | --- |
| 四种正文来源分别存在、同时存在、全空 | 优先级正确，0..n 结果与可用性有依据 | NOT_RUN |
| summary / 群组成员 / 隐藏内容 | 区分准确，不猜不可见数据 | NOT_RUN |
| 仅 MessagingStyle 正文，保存并重建数据库连接 | 原受控正文与解析结果可恢复 | NOT_RUN |
| 旧 v1 数据库升级与特殊字符往返 | 旧可用数据保留，新结构读写一致 | NOT_RUN |
| 真实来源脱敏样本回放 | 结果可追溯到真实提供的字段 | NOT_RUN |

Verify + 真实 Room / 字段提取设备测试；记录 schema 版本与迁移报告。不得只用 JSON 解析通过代替迁移验证。此任务不解决内容版本事务、详情 UI 或任务提取。
