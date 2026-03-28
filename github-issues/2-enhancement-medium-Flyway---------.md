## 🟡 Flyway 数据库迁移已禁用

**严重程度:** MEDIUM
**类别:** enhancement
**发现时间:** 2026-03-28T02:40:21.318Z

### 问题描述
由于迁移脚本问题，Flyway 已被禁用。需要手动管理数据库 schema。





### 复现步骤
1. 检查 application.yml
2. 发现 flyway.enabled=false

### 预期行为
Flyway 应该自动管理数据库迁移

### 实际行为
Flyway 被禁用，需要手动维护表结构



---
*由 Playwright 自动化测试生成*
