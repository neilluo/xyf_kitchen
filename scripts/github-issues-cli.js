#!/usr/bin/env node

/**
 * GitHub Issues CLI 工具
 * 
 * 独立可复用的 GitHub Issues 管理工具
 * 不依赖 OpenClaw Agent 的 memory
 * 
 * 用法:
 *   node github-issues-cli.js create --title "xxx" --body "xxx"
 *   node github-issues-cli.js create-from-file ./issue.md
 *   node github-issues-cli.js list
 *   node github-issues-cli.js close 123
 * 
 * 环境变量:
 *   GITHUB_TOKEN - GitHub Personal Access Token
 *   GITHUB_REPO  - 仓库名 (格式: owner/repo)
 */

const https = require('https');
const fs = require('fs');
const path = require('path');

// 配置
const TOKEN = process.env.GITHUB_TOKEN;
const REPO = process.env.GITHUB_REPO || 'neilluo/xyf_kitchen';
const API_BASE = 'api.github.com';

// 颜色输出
const colors = {
  reset: '\x1b[0m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
};

function log(color, message) {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

// GitHub API 请求
function githubRequest(method, path, data = null) {
  return new Promise((resolve, reject) => {
    if (!TOKEN) {
      reject(new Error('GITHUB_TOKEN not set'));
      return;
    }

    const options = {
      hostname: API_BASE,
      port: 443,
      path: path,
      method: method,
      headers: {
        'Authorization': `token ${TOKEN}`,
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'GitHub-Issues-CLI',
      },
    };

    if (data) {
      options.headers['Content-Type'] = 'application/json';
    }

    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => body += chunk);
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(json);
          } else {
            reject(new Error(`API Error ${res.statusCode}: ${json.message || body}`));
          }
        } catch (e) {
          reject(new Error(`Parse Error: ${e.message}`));
        }
      });
    });

    req.on('error', reject);

    if (data) {
      req.write(JSON.stringify(data));
    }
    req.end();
  });
}

// 创建 Issue
async function createIssue(title, body, labels = []) {
  log('cyan', `Creating issue: ${title}`);
  
  try {
    const issue = await githubRequest('POST', `/repos/${REPO}/issues`, {
      title,
      body,
      labels,
    });
    
    log('green', `✅ Issue #${issue.number} created successfully`);
    log('blue', `   URL: ${issue.html_url}`);
    return issue;
  } catch (error) {
    log('red', `❌ Failed to create issue: ${error.message}`);
    throw error;
  }
}

// 从文件创建 Issue
async function createIssueFromFile(filePath) {
  if (!fs.existsSync(filePath)) {
    log('red', `File not found: ${filePath}`);
    return;
  }

  const content = fs.readFileSync(filePath, 'utf8');
  
  // 解析标题
  const titleMatch = content.match(/^## .+? (.+)$/m);
  const title = titleMatch ? titleMatch[1].trim() : 'Untitled Issue';
  
  // 解析标签
  const severityMatch = content.match(/\*\*严重程度:\*\* (\w+)/i);
  const categoryMatch = content.match(/\*\*类别:\*\* (\w+)/i);
  
  const labels = ['automated'];
  if (severityMatch) labels.push(severityMatch[1].toLowerCase());
  if (categoryMatch) labels.push(categoryMatch[1].toLowerCase());
  
  await createIssue(title, content, labels);
}

// 列出 Issues
async function listIssues(state = 'open') {
  log('cyan', `Listing ${state} issues...`);
  
  try {
    const issues = await githubRequest('GET', `/repos/${REPO}/issues?state=${state}`);
    
    if (issues.length === 0) {
      log('yellow', 'No issues found');
      return;
    }
    
    log('green', `\nFound ${issues.length} issues:\n`);
    
    issues.forEach(issue => {
      const labels = issue.labels.map(l => l.name).join(', ');
      const color = issue.state === 'open' ? 'green' : 'red';
      log(color, `#${issue.number} [${issue.state.toUpperCase()}] ${issue.title}`);
      log('reset', `   Labels: ${labels || 'none'}`);
      log('reset', `   URL: ${issue.html_url}\n`);
    });
  } catch (error) {
    log('red', `❌ Failed to list issues: ${error.message}`);
  }
}

// 关闭 Issue
async function closeIssue(number) {
  log('cyan', `Closing issue #${number}...`);
  
  try {
    const issue = await githubRequest('PATCH', `/repos/${REPO}/issues/${number}`, {
      state: 'closed',
    });
    
    log('green', `✅ Issue #${issue.number} closed`);
  } catch (error) {
    log('red', `❌ Failed to close issue: ${error.message}`);
  }
}

// 添加评论
async function addComment(number, body) {
  log('cyan', `Adding comment to issue #${number}...`);
  
  try {
    const comment = await githubRequest('POST', `/repos/${REPO}/issues/${number}/comments`, {
      body,
    });
    
    log('green', `✅ Comment added`);
    log('blue', `   URL: ${comment.html_url}`);
  } catch (error) {
    log('red', `❌ Failed to add comment: ${error.message}`);
  }
}

// 批量创建 Issues
async function createIssuesFromDirectory(dirPath) {
  if (!fs.existsSync(dirPath)) {
    log('red', `Directory not found: ${dirPath}`);
    return;
  }

  const files = fs.readdirSync(dirPath).filter(f => f.endsWith('.md') && !f.includes('SUMMARY'));
  
  log('cyan', `Found ${files.length} issue files in ${dirPath}`);
  
  for (const file of files) {
    const filePath = path.join(dirPath, file);
    log('magenta', `\nProcessing: ${file}`);
    
    try {
      await createIssueFromFile(filePath);
    } catch (error) {
      log('red', `Failed to process ${file}: ${error.message}`);
    }
  }
}

// 验证 Token
async function verifyToken() {
  try {
    const user = await githubRequest('GET', '/user');
    log('green', `✅ Token verified for user: ${user.login}`);
    return true;
  } catch (error) {
    log('red', `❌ Token verification failed: ${error.message}`);
    return false;
  }
}

// 显示帮助
function showHelp() {
  console.log(`
${colors.cyan}GitHub Issues CLI Tool${colors.reset}

${colors.yellow}Usage:${colors.reset}
  node github-issues-cli.js <command> [options]

${colors.yellow}Commands:${colors.reset}
  create --title "Title" --body "Body" [--labels "label1,label2"]  Create a new issue
  create-from-file <file.md>                                      Create issue from markdown file
  create-from-dir <directory>                                     Create issues from all markdown files
  list [--state open|closed|all]                                  List issues
  close <number>                                                  Close an issue
  comment <number> --body "Comment"                               Add comment to issue
  verify                                                          Verify GitHub token

${colors.yellow}Environment Variables:${colors.reset}
  GITHUB_TOKEN    GitHub Personal Access Token (required)
  GITHUB_REPO     Repository name in format: owner/repo (default: neilluo/xyf_kitchen)

${colors.yellow}Examples:${colors.reset}
  export GITHUB_TOKEN="ghp_xxxxxx"
  
  node github-issues-cli.js verify
  node github-issues-cli.js create --title "Bug found" --body "Description here" --labels "bug,critical"
  node github-issues-cli.js create-from-file ./github-issues/issue.md
  node github-issues-cli.js create-from-dir ./github-issues
  node github-issues-cli.js list
  node github-issues-cli.js close 123
  node github-issues-cli.js comment 123 --body "Fixed in PR #456"
`);
}

// 解析参数
function getArgValue(args, flag) {
  const index = args.indexOf(flag);
  return index !== -1 && args[index + 1] ? args[index + 1] : null;
}

// 主函数
async function main() {
  const args = process.argv.slice(2);
  const command = args[0];

  if (!command || command === '--help' || command === '-h') {
    showHelp();
    return;
  }

  switch (command) {
    case 'verify':
      await verifyToken();
      break;

    case 'create': {
      const title = getArgValue(args, '--title');
      const body = getArgValue(args, '--body');
      const labelsStr = getArgValue(args, '--labels');
      
      if (!title || !body) {
        log('red', 'Error: --title and --body are required');
        showHelp();
        return;
      }
      
      const labels = labelsStr ? labelsStr.split(',').map(l => l.trim()) : [];
      await createIssue(title, body, labels);
      break;
    }

    case 'create-from-file': {
      const filePath = args[1];
      if (!filePath) {
        log('red', 'Error: file path is required');
        return;
      }
      await createIssueFromFile(filePath);
      break;
    }

    case 'create-from-dir': {
      const dirPath = args[1] || './github-issues';
      await createIssuesFromDirectory(dirPath);
      break;
    }

    case 'list': {
      const state = getArgValue(args, '--state') || 'open';
      await listIssues(state);
      break;
    }

    case 'close': {
      const number = args[1];
      if (!number) {
        log('red', 'Error: issue number is required');
        return;
      }
      await closeIssue(number);
      break;
    }

    case 'comment': {
      const number = args[1];
      const body = getArgValue(args, '--body');
      
      if (!number || !body) {
        log('red', 'Error: issue number and --body are required');
        return;
      }
      await addComment(number, body);
      break;
    }

    default:
      log('red', `Unknown command: ${command}`);
      showHelp();
  }
}

// 运行
main().catch(error => {
  log('red', `Fatal error: ${error.message}`);
  process.exit(1);
});
