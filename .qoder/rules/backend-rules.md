---
trigger: "**/*.java"
---

# Backend Rules (Java/Spring Boot)

## DDD Layering — Domain Must Be Pure Java
**Do**: `domain/` 包只有纯 Java 类——实体、值对象、领域事件、仓储接口，无任何框架注解。
```java
// 正确 — 纯值对象
public record VideoId(String value) {}

// 正确 — 仓储接口
public interface VideoRepository { Video findById(VideoId id); }
```
**Don't**: 在 domain 层引入 `@Service`、`@Autowired`、`@Entity`、MyBatis 注解。
```java
// 错误 — domain 层不应该有 Spring 依赖
@Service
public class Video { @Autowired private MyService svc; }
```
**Self-check**: domain 包的 import 语句只包含 `java.*`、项目内部包、第三方纯库（如 Guava）。

## ApiResponse Wrapper — All REST Endpoints
**Do**: 所有 Controller 方法返回 `ApiResponse<T>` 包装。
```java
@PostMapping("/videos")
public ApiResponse<VideoDto> create(@Valid @RequestBody CreateVideoReq req) {
    return ApiResponse.success(videoService.create(req));
}
```
**Don't**: 直接返回 DTO 或裸对象。
```java
// 错误 — 未包装
@PostMapping("/videos")
public VideoDto create(@RequestBody CreateVideoReq req) { ... }
```
**Self-check**: 每个 `@PostMapping`/`@GetMapping`/`@DeleteMapping` 方法的返回类型必须是 `ApiResponse<?>`。

## Flyway Only — Database Changes
**Do**: 数据库变更只通过 Flyway migration 脚本（`V{N}__{description}.sql`）。
```sql
-- V2__add_video_duration.sql
ALTER TABLE video ADD COLUMN duration_seconds BIGINT;
```
**Don't**: 直接写 DDL 或手动改表结构。
```sql
-- 错误 — 不应该手写 ALTER TABLE 直接执行
-- 错误 — 不应该用 ORM auto-ddl 自动生成
```
**Self-check**: 每次数据库变更对应一个新的 `V{N}__*.sql` 文件，在 `grace-platform/src/main/resources/db/migration/`。

## Domain Events — Cross-Context Communication
**Do**: 限界上下文间通过 `ApplicationEventPublisher` 发布领域事件通信。
```java
eventPublisher.publish(new VideoUploadedEvent(videoId));
```
**Don't**: 一个上下文直接 import 另一个上下文的类。
```java
// 错误 — 跨上下文直接依赖
import com.grace.platform.metadata.MetadataService;
```
**Self-check**: context 包下的 import 不能出现 `com.grace.platform.` + 其他 context 名。

## Business Exception — Type-Safe Errors
**Do**: 使用 `ErrorCode` 枚举 + `BusinessException`。
```java
throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND, videoId);
```
**Don't**: 返回魔法数字、通用 RuntimeException、或在 Controller 吞异常。
**Self-check**: 每个自定义异常类继承 `BusinessException`，错误码来自 `ErrorCode` 枚举。

## Naming — snake_case DB, camelCase Java
**Do**: 数据库字段 `snake_case`，Java 字段 `camelCase`，MyBatis 自动映射。
**Don't**: 数据库和 Java 使用相同的命名风格。
**Self-check**: 每个 entity 字段名和对应的 Mapper XML 列名风格不同。

## Post-Coding Verification
**Do**: 编码后必执行：
```bash
mvn clean compile   # Tier 1 — 捕获语法/类型错误
```
有 domain 层变更加跑：
```bash
mvn test -Dtest="*UnitTest,*PropertyTest"
```
有持久层/API 适配器变更加跑：
```bash
mvn test -Dtest="*IntegrationTest"
```
**Self-check**: 每次任务完成后至少通过 Tier 1（编译）。
