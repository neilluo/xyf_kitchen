#!/usr/bin/env node

/**
 * Playwright 测试运行脚本
 * 
 * 用法:
 *   node run-tests.js          # 运行所有测试
 *   node run-tests.js --ui     # 运行 UI 模式
 *   node run-tests.js --debug  # 运行调试模式
 *   node run-tests.js --report # 生成报告
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// 确保测试目录存在
const testResultsDir = path.join(__dirname, '../../test-results');
if (!fs.existsSync(testResultsDir)) {
  fs.mkdirSync(testResultsDir, { recursive: true });
}

// 解析参数
const args = process.argv.slice(2);
const isUiMode = args.includes('--ui');
const isDebug = args.includes('--debug');
const isReport = args.includes('--report');
const isHelp = args.includes('--help') || args.includes('-h');

if (isHelp) {
  console.log(`
🎭 Grace Platform Playwright 测试工具

用法:
  node run-tests.js [选项]

选项:
  --ui       打开 Playwright UI 模式
  --debug    运行调试模式（有界面）
  --report   只生成 HTML 报告
  --help     显示帮助信息

示例:
  node run-tests.js              # 运行所有测试
  node run-tests.js --ui         # 打开 UI 模式
  node run-tests.js --debug      # 调试模式
`);
  process.exit(0);
}

// 检查服务是否运行
function checkServices() {
  console.log('🔍 检查服务状态...');
  
  try {
    // 检查后端
    execSync('curl -s http://localhost:8080/api/dashboard/overview > /dev/null', { stdio: 'ignore' });
    console.log('✅ 后端服务运行正常 (http://localhost:8080)');
  } catch {
    console.log('⚠️  后端服务未启动，请先启动后端服务');
    console.log('   运行: cd grace-platform && java -jar target/grace-platform-0.1.0-SNAPSHOT.jar');
  }
  
  try {
    // 检查前端
    execSync('curl -s http://localhost:3001 > /dev/null', { stdio: 'ignore' });
    console.log('✅ 前端服务运行正常 (http://localhost:3001)');
  } catch {
    console.log('⚠️  前端服务未启动，请先启动前端服务');
    console.log('   运行: cd grace-frontend && npm run dev');
  }
}

// 运行测试
function runTests() {
  console.log('🚀 启动 Playwright 测试...\n');
  
  let command = 'npx playwright test';
  
  if (isUiMode) {
    command = 'npx playwright test --ui';
  } else if (isDebug) {
    command = 'npx playwright test --headed --debug';
  } else if (isReport) {
    command = 'npx playwright show-report';
  }
  
  try {
    execSync(command, {
      cwd: path.join(__dirname, '../..'),
      stdio: 'inherit',
      env: {
        ...process.env,
        PW_TEST_HTML_REPORT_OPEN: 'never',
      }
    });
    
    if (!isReport) {
      console.log('\n✅ 测试完成！');
      console.log(`📊 查看报告: npx playwright show-report`);
      console.log(`📁 测试结果: ${testResultsDir}`);
    }
  } catch (error) {
    console.log('\n❌ 测试失败');
    console.log(`📊 查看报告: npx playwright show-report`);
    process.exit(1);
  }
}

// 生成 GitHub Issues
function generateIssues() {
  console.log('\n📝 生成 GitHub Issues...');
  
  const { GitHubIssuesReporter } = require('./github-issues-reporter');
  const reporter = new GitHubIssuesReporter();
  
  // 从测试结果中读取问题
  const resultsPath = path.join(__dirname, '../../test-results/results.json');
  if (fs.existsSync(resultsPath)) {
    const results = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));
    
    results.suites?.forEach(suite => {
      suite.specs?.forEach(spec => {
        spec.tests?.forEach(test => {
          test.results?.forEach(result => {
            if (result.status === 'failed' || result.status === 'timedOut') {
              reporter.addIssue({
                title: `测试失败: ${spec.title}`,
                description: `测试用例 "${spec.title}" 在 "${suite.title}" 中失败。`,
                severity: 'high',
                category: 'bug',
                errorMessage: result.error?.message || 'Unknown error',
                stepsToReproduce: [
                  `运行测试: ${spec.title}`,
                  `查看错误: ${result.error?.message || 'No error message'}`
                ],
                expectedBehavior: '测试应该通过',
                actualBehavior: `测试失败: ${result.status}`
              });
            }
          });
        });
      });
    });
  }
  
  // 添加已知问题
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
    actualBehavior: 'API 返回 500 错误'
  });
  
  reporter.saveIssues();
  console.log('✅ GitHub Issues 已生成到 github-issues/ 目录');
}

// 主流程
console.log('🎭 Grace Platform Playwright 测试\n');

checkServices();

if (!isReport) {
  console.log('');
}

runTests();

if (!isUiMode && !isDebug && !isReport) {
  generateIssues();
}
