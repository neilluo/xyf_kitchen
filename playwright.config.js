// @ts-check
const { defineConfig, devices } = require('@playwright/test');

/**
 * @see https://playwright.dev/docs/test-configuration
 */
module.exports = defineConfig({
  testDir: './tests/e2e',
  
  /* 每个测试的超时时间 */
  timeout: 60 * 1000, // 60 seconds
  
  /* 断言超时 */
  expect: {
    timeout: 10000, // 10 seconds
  },
  
  /* 测试并行数 */
  workers: process.env.CI ? 1 : undefined,
  
  /* 测试报告 */
  reporter: [
    ['html', { open: 'never' }],
    ['json', { outputFile: 'test-results/results.json' }],
    ['list'],
  ],
  
  /* 共享配置 */
  use: {
    /* 基础 URL */
    baseURL: 'http://localhost:3001',
    
    /* 是否显示浏览器 */
    headless: true,
    
    /* 视口大小 */
    viewport: { width: 1280, height: 720 },
    
    /* 截图策略 */
    screenshot: 'only-on-failure',
    
    /* 视频录制 */
    video: 'retain-on-failure',
    
    /* 追踪 */
    trace: 'on-first-retry',
    
    /* 动作超时 */
    actionTimeout: 15000,
    
    /* 导航超时 */
    navigationTimeout: 30000,
  },

  /* 项目配置 */
  projects: [
    {
      name: 'chromium',
      use: { 
        ...devices['Desktop Chrome'],
        launchOptions: {
          args: ['--disable-web-security', '--disable-features=IsolateOrigins,site-per-process'],
        },
      },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    /* 移动端测试 */
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
  ],

  /* 测试前启动服务 */
  webServer: {
    command: 'cd /home/neilluo/Desktop/xyf_kitchen/grace-frontend && npm run dev',
    url: 'http://localhost:3001',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
});
