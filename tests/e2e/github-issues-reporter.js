/**
 * GitHub Issues 自动生成器
 * 
 * 将 Playwright 测试结果转换为 GitHub Issues
 */

const fs = require('fs');
const path = require('path');

class GitHubIssuesReporter {
  constructor() {
    this.issues = [];
    this.testResultsDir = './test-results';
  }

  /**
   * 添加问题到列表
   */
  addIssue({
    title,
    description,
    severity = 'medium', // low, medium, high, critical
    category = 'bug', // bug, feature, enhancement
    labels = [],
    screenshots = [],
    stepsToReproduce = [],
    expectedBehavior = '',
    actualBehavior = '',
    apiEndpoint = '',
    errorMessage = ''
  }) {
    this.issues.push({
      title,
      description,
      severity,
      category,
      labels: [...labels, severity, category],
      screenshots,
      stepsToReproduce,
      expectedBehavior,
      actualBehavior,
      apiEndpoint,
      errorMessage,
      createdAt: new Date().toISOString()
    });
  }

  /**
   * 生成 GitHub Issue Markdown
   */
  generateIssueMarkdown(issue) {
    const severityEmoji = {
      critical: '🔴',
      high: '🟠',
      medium: '🟡',
      low: '🟢'
    };

    return `## ${severityEmoji[issue.severity]} ${issue.title}

**严重程度:** ${issue.severity.toUpperCase()}
**类别:** ${issue.category}
**发现时间:** ${issue.createdAt}

### 问题描述
${issue.description}

${issue.errorMessage ? `### 错误信息
\`\`\`
${issue.errorMessage}
\`\`\`
` : ''}

${issue.apiEndpoint ? `### 相关 API
\`${issue.apiEndpoint}\`
` : ''}

### 复现步骤
${issue.stepsToReproduce.map((step, i) => `${i + 1}. ${step}`).join('\n')}

### 预期行为
${issue.expectedBehavior}

### 实际行为
${issue.actualBehavior}

${issue.screenshots.length > 0 ? `### 截图
${issue.screenshots.map(s => `![${s}](${s})`).join('\n')}
` : ''}

---
*由 Playwright 自动化测试生成*
`;
  }

  /**
   * 保存所有 Issues 到文件
   */
  saveIssues(outputDir = './github-issues') {
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true });
    }

    this.issues.forEach((issue, index) => {
      const filename = `${index + 1}-${issue.category}-${issue.severity}-${issue.title.replace(/[^a-zA-Z0-9]/g, '-').substring(0, 50)}.md`;
      const filepath = path.join(outputDir, filename);
      const markdown = this.generateIssueMarkdown(issue);
      fs.writeFileSync(filepath, markdown);
      console.log(`✅ Issue 已保存: ${filepath}`);
    });

    // 生成汇总报告
    this.generateSummaryReport(outputDir);
  }

  /**
   * 生成汇总报告
   */
  generateSummaryReport(outputDir) {
    const summary = `# 测试问题汇总报告

**生成时间:** ${new Date().toISOString()}
**发现问题总数:** ${this.issues.length}

## 按严重程度分类

| 严重程度 | 数量 |
|---------|------|
| 🔴 Critical | ${this.issues.filter(i => i.severity === 'critical').length} |
| 🟠 High | ${this.issues.filter(i => i.severity === 'high').length} |
| 🟡 Medium | ${this.issues.filter(i => i.severity === 'medium').length} |
| 🟢 Low | ${this.issues.filter(i => i.severity === 'low').length} |

## 按类别分类

| 类别 | 数量 |
|------|------|
| Bug | ${this.issues.filter(i => i.category === 'bug').length} |
| Feature | ${this.issues.filter(i => i.category === 'feature').length} |
| Enhancement | ${this.issues.filter(i => i.category === 'enhancement').length} |

## 问题列表

${this.issues.map((issue, i) => {
  const emoji = { critical: '🔴', high: '🟠', medium: '🟡', low: '🟢' };
  return `${i + 1}. ${emoji[issue.severity]} [${issue.category.toUpperCase()}] ${issue.title}`;
}).join('\n')}

## 快速修复建议

### 高优先级
${this.issues.filter(i => i.severity === 'critical' || i.severity === 'high').map(i => `- [ ] ${i.title}`).join('\n') || '无'}

### 中优先级
${this.issues.filter(i => i.severity === 'medium').map(i => `- [ ] ${i.title}`).join('\n') || '无'}

### 低优先级
${this.issues.filter(i => i.severity === 'low').map(i => `- [ ] ${i.title}`).join('\n') || '无'}

---
*由 Playwright 自动化测试生成*
`;

    fs.writeFileSync(path.join(outputDir, '00-SUMMARY.md'), summary);
    console.log(`✅ 汇总报告已保存: ${path.join(outputDir, '00-SUMMARY.md')}`);
  }
}

// 导出用于测试文件
module.exports = { GitHubIssuesReporter };

// 如果直接运行此文件，执行示例
if (require.main === module) {
  const reporter = new GitHubIssuesReporter();
  
  // 示例：添加一些已知问题
  reporter.addIssue({
    title: 'POST /api/videos/upload/init 返回 500 错误',
    description: '视频上传初始化接口返回 500 Internal Server Error，导致无法开始上传流程。',
    severity: 'critical',
    category: 'bug',
    apiEndpoint: 'POST /api/videos/upload/init',
    errorMessage: 'Internal server error',
    stepsToReproduce: [
      '访问 http://localhost:3001/upload',
      '点击"选择文件"按钮',
      '选择任意视频文件',
      '观察网络请求'
    ],
    expectedBehavior: 'API 应该返回 200 并创建上传会话',
    actualBehavior: 'API 返回 500 错误',
    screenshots: ['test-results/upload-error.png']
  });

  reporter.addIssue({
    title: 'Flyway 数据库迁移已禁用',
    description: '由于迁移脚本问题，Flyway 已被禁用。需要手动管理数据库 schema。',
    severity: 'medium',
    category: 'enhancement',
    stepsToReproduce: [
      '检查 application.yml',
      '发现 flyway.enabled=false'
    ],
    expectedBehavior: 'Flyway 应该自动管理数据库迁移',
    actualBehavior: 'Flyway 被禁用，需要手动维护表结构'
  });

  reporter.saveIssues();
  console.log('\n📋 使用说明:');
  console.log('1. 在 Playwright 测试中使用 GitHubIssuesReporter 记录问题');
  console.log('2. 运行测试后，检查 github-issues/ 目录');
  console.log('3. 将生成的 .md 文件内容复制到 GitHub Issues');
}
