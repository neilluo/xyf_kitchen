# GitHub Issues CLI 工具

> 独立可复用的 GitHub Issues 管理工具，不依赖 OpenClaw Agent

## 📦 安装

无需安装，直接使用 Node.js 运行：

```bash
chmod +x github-issues-cli.js
```

## 🔧 配置

### 1. 设置环境变量

```bash
export GITHUB_TOKEN="ghp_你的token"
export GITHUB_REPO="neilluo/xyf_kitchen"  # 可选，默认就是这个
```

### 2. 验证配置

```bash
node github-issues-cli.js verify
```

## 🚀 用法

### 创建单个 Issue

```bash
node github-issues-cli.js create \
  --title "Bug found" \
  --body "Description here" \
  --labels "bug,critical"
```

### 从文件创建 Issue

```bash
# 从单个文件
node github-issues-cli.js create-from-file ./github-issues/issue.md

# 从整个目录
node github-issues-cli.js create-from-dir ./github-issues
```

### 列出 Issues

```bash
# 列出开放的 Issues
node github-issues-cli.js list

# 列出已关闭的
node github-issues-cli.js list --state closed

# 列出所有
node github-issues-cli.js list --state all
```

### 关闭 Issue

```bash
node github-issues-cli.js close 123
```

### 添加评论

```bash
node github-issues-cli.js comment 123 --body "Fixed in PR #456"
```

## 📁 Issue 文件格式

创建 `github-issues/xxx.md`：

```markdown
## 🔴 问题标题

**严重程度:** CRITICAL
**类别:** bug

### 问题描述
详细描述...

### 复现步骤
1. 步骤1
2. 步骤2

### 预期行为
...

### 实际行为
...
```

工具会自动解析：
- 标题（从 `## ` 后面提取）
- 严重程度（从 `**严重程度:**` 提取）
- 类别（从 `**类别:**` 提取）
- 标签（自动添加 `automated` + 严重程度 + 类别）

## 🎯 使用场景

### 场景 1：CI/CD 自动创建 Issues

```yaml
# .github/workflows/create-issues.yml
name: Create Issues from Test Results
on:
  workflow_dispatch:
  
jobs:
  create-issues:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Create Issues
        run: node scripts/github-issues-cli.js create-from-dir ./github-issues
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 场景 2：Playwright 测试失败自动创建

```javascript
// 在 Playwright 测试中使用
const { execSync } = require('child_process');

if (testFailed) {
  execSync(`node scripts/github-issues-cli.js create --title "Test failed: ${testName}" --body "..."`);
}
```

### 场景 3：手动批量创建

```bash
# 批量创建所有预定义的 Issues
node scripts/github-issues-cli.js create-from-dir ./github-issues
```

## 🔒 安全

- Token 只保存在环境变量中
- 不会记录到日志
- 支持最小权限原则（只需 `repo` 权限）

## 📚 依赖

- Node.js >= 14
- 无需额外 npm 包（使用原生 https 模块）

## 📝 License

MIT
