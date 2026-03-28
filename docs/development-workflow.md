# 开发工作流程指南

> 避免返工的标准操作流程

---

## 🎯 黄金法则

**先查代码，再写文档，最后实现**

---

## 📋 标准流程

### Phase 1: 调研（30% 时间）

```
□ 查看现有相关代码
  - 找到所有相关类文件
  - 理解接口定义
  - 查看已有实现

□ 查看数据库结构
  - 相关表结构
  - 字段定义
  - 关联关系

□ 查看配置文件
  - application.yml
  - 环境变量
  - 已有配置项
```

### Phase 2: 设计（20% 时间）

```
□ 确定接口契约
  - 输入参数
  - 返回值
  - 异常处理

□ 确定与现有代码的集成点
  - 调用关系
  - 依赖注入
  - 事件发布

□ 编写最小化文档
  - 接口定义
  - 关键流程
  - 注意事项
```

### Phase 3: 实现（40% 时间）

```
□ 编写代码
  - 遵循现有代码风格
  - 复用已有组件
  - 保持接口兼容

□ 编写测试
  - 单元测试
  - 集成测试

□ 本地验证
  - 编译通过
  - 测试通过
```

### Phase 4: 提交（10% 时间）

```
□ 代码审查
  - 自我审查
  - 检查兼容性

□ 提交代码
  - 清晰的 commit message
  - 关联 Issue

□ 更新文档
  - API 文档
  - 配置说明
```

---

## ❌ 常见错误

### 错误 1：不看现有代码直接写
```java
// ❌ 错误：创建新的类，与现有接口不兼容
public class MyNewService {
    public String doSomething() { ... }
}

// ✅ 正确：实现已有接口
public class MyNewService implements ExistingInterface {
    @Override
    public ExpectedResult doSomething() { ... }
}
```

### 错误 2：文档和代码脱节
```java
// 文档说：
public record Result(String taskId, String status) {}

// 实际代码需要：
public class Result {
    public String getTaskId() { ... }
    public YouTubeUploadStatus getStatus() { ... }
}
```

### 错误 3：重复造轮子
```java
// ❌ 错误：创建新的加密工具
public class MyEncryptionUtil { ... }

// ✅ 正确：使用已有的加密服务
@Autowired
private EncryptionService encryptionService;
```

---

## 🔧 实用命令

### 查找相关代码
```bash
# 查找包含关键字的文件
grep -r "YouTubeUpload" --include="*.java" src/

# 查找类定义
find src -name "*YouTube*.java"

# 查看接口实现
grep -r "implements YouTubeApiAdapter" --include="*.java" src/
```

### 检查兼容性
```bash
# 编译检查
mvn clean compile

# 测试检查
mvn test

# 依赖检查
mvn dependency:tree
```

---

## 📚 案例：YouTube 上传功能

### ❌ 错误流程
```
1. 直接参考外部教程写代码
2. 创建新的 YouTubeUploadResult 类
3. 发现与 YouTubeDistributor 不兼容
4. 返工修改
```

### ✅ 正确流程
```
1. 查看 YouTubeDistributor.java
   - 发现它使用 YouTubeApiAdapter
   - 查看 YouTubeUploadResult 的定义

2. 查看 YouTubeApiAdapter.java
   - 理解接口方法
   - 查看返回值类型

3. 设计实现方案
   - 实现 YouTubeApiAdapter 接口
   - 复用现有的 YouTubeUploadResult

4. 编写代码
   - 实现接口方法
   - 保持返回值兼容

5. 测试验证
   - 编译通过
   - YouTubeDistributor 能正常工作
```

---

## 🎯 检查清单

在开始任何功能开发前，确认：