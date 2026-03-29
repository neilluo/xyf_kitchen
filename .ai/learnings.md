# 学习笔记（Running Notebook）

> Agent 在开发过程中发现的经验，每条 1-2 行。新会话开始时加载此文件。

## 有效模式

- （开发过程中积累）

## 踩坑记录

- domain 层不能引入 Spring 注解 — `@Service`/`@Autowired` 放 infrastructure 或 application 层
- 组件不能直接调用 axios — 必须走 `src/api/` → `src/hooks/` → 组件链路
- 禁止第三方 UI 库 — 所有 UI 组件自建于 `src/components/ui/`
- REST 端点必须用 `ApiResponse<T>` 包装 — 不能直接返回实体
- 数据库变更只能通过 Flyway `V{N}__{description}.sql` — 禁止手写 DDL
- 跨上下文不能直接 import — 通过领域事件通信
- **实现新功能前必须先查看现有代码** — 避免与已有接口不兼容导致返工（如 YouTube 上传功能）
- **文档先行但必须基于现有代码** — 外部教程仅供参考，接口定义以项目代码为准
- **禁止重复造轮子** — 添加新类前检查是否已有类似功能，避免代码库膨胀（如 YouTubeConfig vs YouTubeProperties）
- **保持架构一致性** — 新功能必须遵循现有设计模式，不能另起炉灶（如 OAuthService 已有接口，不要创建新的）
- **Repository save 方法避免数据库查询判断** — 使用领域对象状态（如 createdAt.equals(updatedAt)）判断新旧，减少数据库往返
- **禁止 RuntimeException** — 所有异常必须使用领域异常体系（BusinessException/InfrastructureException/EntityNotFoundException）
- **预生成 ID 的实体不能通过 id == null 判断新旧** — 需要在领域层设计判断逻辑（如时间戳比较）
- **异常类要内聚到 shared 模块** — FileOperationException/EncryptionException 等复用异常统一放在 shared.infrastructure.exception

## 文档澄清

- （当文档描述模糊时记录正确解读）

## 环境适配

- 当前环境使用 Java 17，项目需 Java 21 — 临时降级 POM 到 Java 17 验证编译，后续需升级回 Java 21
- 2026-03-27: 尝试升级 POM 到 Java 21 失败，环境仅支持 Java 17 — 保持 Java 17 配置，Record Patterns 等 preview features 暂不可用
