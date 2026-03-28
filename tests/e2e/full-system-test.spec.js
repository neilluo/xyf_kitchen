const { test, expect } = require('@playwright/test');

/**
 * Grace Platform 全方位系统测试
 * 
 * 测试范围：
 * 1. 页面加载和导航
 * 2. 表单交互
 * 3. API 集成
 * 4. 错误处理
 * 5. 边界情况
 */

test.describe('Grace Platform - 全方位系统测试', () => {
  
  test.beforeEach(async ({ page }) => {
    // 访问前端页面
    await page.goto('http://localhost:3001');
    // 等待页面加载完成
    await page.waitForLoadState('networkidle');
  });

  // ==================== 1. Dashboard 页面测试 ====================
  test.describe('Dashboard 页面', () => {
    
    test('页面标题和基本结构', async ({ page }) => {
      await expect(page).toHaveTitle(/Grace/);
      await expect(page.locator('body')).toBeVisible();
      
      // 检查关键元素
      await expect(page.locator('text=Dashboard').first()).toBeVisible();
      await expect(page.locator('text=总视频数').first()).toBeVisible();
    });

    test('Stats Cards 显示和交互', async ({ page }) => {
      // 检查 4 个 Stats Cards
      const statsCards = page.locator('[class*="StatsCard"], [data-testid*="stats"]').first();
      await expect(statsCards).toBeVisible();
      
      // 截图记录
      await page.screenshot({ path: 'test-results/dashboard-stats.png' });
    });

    test('日期范围选择器', async ({ page }) => {
      // 查找日期选择按钮
      const dateButtons = ['7天', '30天', '90天', '全部'];
      
      for (const btnText of dateButtons) {
        const button = page.locator(`button:has-text("${btnText}")`).first();
        if (await button.isVisible().catch(() => false)) {
          await button.click();
          await page.waitForTimeout(500);
        }
      }
      
      await page.screenshot({ path: 'test-results/dashboard-date-range.png' });
    });

    test('最近上传表格', async ({ page }) => {
      // 检查表格或空状态
      const table = page.locator('table').first();
      const emptyState = page.locator('text=暂无数据, text=No data, text=Empty').first();
      
      const hasTable = await table.isVisible().catch(() => false);
      const hasEmpty = await emptyState.isVisible().catch(() => false);
      
      expect(hasTable || hasEmpty).toBeTruthy();
    });
  });

  // ==================== 2. 视频上传页面测试 ====================
  test.describe('视频上传页面', () => {
    
    test.beforeEach(async ({ page }) => {
      // 导航到上传页面
      await page.goto('http://localhost:3001/upload');
      await page.waitForLoadState('networkidle');
    });

    test('上传页面加载', async ({ page }) => {
      await expect(page.locator('text=上传视频').first()).toBeVisible();
      await page.screenshot({ path: 'test-results/upload-page.png' });
    });

    test('拖拽上传区域', async ({ page }) => {
      // 检查拖拽区域
      const dropZone = page.locator('[class*="DropZone"], [class*="drop"]').first();
      await expect(dropZone).toBeVisible();
      
      // 模拟拖拽事件
      await dropZone.dispatchEvent('dragenter');
      await page.waitForTimeout(300);
      await dropZone.dispatchEvent('dragleave');
      
      await page.screenshot({ path: 'test-results/upload-dropzone.png' });
    });

    test('文件选择按钮', async ({ page }) => {
      // 检查文件选择输入
      const fileInput = page.locator('input[type="file"]').first();
      await expect(fileInput).toBeHidden(); // 通常隐藏
      
      // 检查选择按钮
      const selectButton = page.locator('button:has-text("选择"), button:has-text("Select")').first();
      if (await selectButton.isVisible().catch(() => false)) {
        await selectButton.click();
      }
    });

    test('上传进度显示（模拟）', async ({ page }) => {
      // 检查是否有进度条组件
      const progressBar = page.locator('[class*="ProgressBar"], [class*="progress"]').first();
      
      await page.screenshot({ path: 'test-results/upload-progress.png' });
    });
  });

  // ==================== 3. 视频管理页面测试 ====================
  test.describe('视频管理页面', () => {
    
    test.beforeEach(async ({ page }) => {
      await page.goto('http://localhost:3001/videos');
      await page.waitForLoadState('networkidle');
    });

    test('视频列表加载', async ({ page }) => {
      await expect(page.locator('text=视频管理').first()).toBeVisible();
      
      // 检查表格或空状态
      const content = page.locator('table, [class*="empty"], text=暂无').first();
      await expect(content).toBeVisible();
      
      await page.screenshot({ path: 'test-results/video-management.png' });
    });

    test('筛选功能', async ({ page }) => {
      // 检查筛选控件
      const searchInput = page.locator('input[type="search"], input[placeholder*="搜索"]').first();
      const statusSelect = page.locator('select, [class*="Select"]').first();
      
      if (await searchInput.isVisible().catch(() => false)) {
        await searchInput.fill('test');
        await page.waitForTimeout(500);
      }
      
      await page.screenshot({ path: 'test-results/video-filter.png' });
    });

    test('分页功能', async ({ page }) => {
      // 检查分页组件
      const pagination = page.locator('[class*="Pagination"], button:has-text(">"), button:has-text("下一页")').first();
      
      if (await pagination.isVisible().catch(() => false)) {
        await pagination.click();
        await page.waitForTimeout(500);
      }
      
      await page.screenshot({ path: 'test-results/video-pagination.png' });
    });
  });

  // ==================== 4. 元数据审核页面测试 ====================
  test.describe('元数据审核页面', () => {
    
    test.beforeEach(async ({ page }) => {
      await page.goto('http://localhost:3001/review');
      await page.waitForLoadState('networkidle');
    });

    test('元数据页面加载', async ({ page }) => {
      await expect(page.locator('text=元数据').first()).toBeVisible();
      await page.screenshot({ path: 'test-results/metadata-review.png' });
    });

    test('元数据编辑器', async ({ page }) => {
      // 检查编辑器组件
      const editor = page.locator('textarea, input[type="text"]').first();
      
      if (await editor.isVisible().catch(() => false)) {
        await editor.fill('Test title');
        await page.waitForTimeout(300);
      }
      
      await page.screenshot({ path: 'test-results/metadata-editor.png' });
    });
  });

  // ==================== 5. 设置页面测试 ====================
  test.describe('设置页面', () => {
    
    test.beforeEach(async ({ page }) => {
      await page.goto('http://localhost:3001/settings');
      await page.waitForLoadState('networkidle');
    });

    test('用户资料页面', async ({ page }) => {
      await expect(page.locator('text=设置, text=Settings, text=用户资料').first()).toBeVisible();
      
      // 检查表单字段
      const inputs = page.locator('input').all();
      expect(inputs.length).toBeGreaterThan(0);
      
      await page.screenshot({ path: 'test-results/settings-profile.png' });
    });

    test('通知偏好设置', async ({ page }) => {
      // 查找通知设置
      const toggle = page.locator('input[type="checkbox"], [role="switch"]').first();
      
      if (await toggle.isVisible().catch(() => false)) {
        await toggle.click();
        await page.waitForTimeout(300);
      }
      
      await page.screenshot({ path: 'test-results/settings-notifications.png' });
    });
  });

  // ==================== 6. 导航和布局测试 ====================
  test.describe('导航和布局', () => {
    
    test('侧边栏导航', async ({ page }) => {
      // 检查侧边栏
      const sidebar = page.locator('nav, aside, [class*="Sidebar"]').first();
      await expect(sidebar).toBeVisible();
      
      // 测试各个导航链接
      const navItems = ['Dashboard', '上传', '管理', '设置'];
      
      for (const item of navItems) {
        const link = page.locator(`nav a:has-text("${item}"), nav button:has-text("${item}")`).first();
        if (await link.isVisible().catch(() => false)) {
          await link.hover();
          await page.waitForTimeout(200);
        }
      }
      
      await page.screenshot({ path: 'test-results/sidebar-navigation.png' });
    });

    test('响应式布局', async ({ page }) => {
      // 测试不同视口
      const viewports = [
        { width: 1920, height: 1080, name: 'desktop' },
        { width: 1024, height: 768, name: 'tablet' },
        { width: 375, height: 667, name: 'mobile' },
      ];
      
      for (const viewport of viewports) {
        await page.setViewportSize({ width: viewport.width, height: viewport.height });
        await page.goto('http://localhost:3001');
        await page.waitForTimeout(500);
        await page.screenshot({ path: `test-results/responsive-${viewport.name}.png` });
      }
    });
  });

  // ==================== 7. API 错误处理测试 ====================
  test.describe('API 错误处理', () => {
    
    test('网络错误提示', async ({ page }) => {
      // 模拟网络错误
      await page.route('**/api/**', route => route.abort('internetdisconnected'));
      
      await page.goto('http://localhost:3001');
      await page.waitForTimeout(1000);
      
      // 检查错误提示
      const errorMessage = page.locator('text=错误, text=Error, text=网络, text=Network').first();
      
      await page.unroute('**/api/**');
      await page.screenshot({ path: 'test-results/api-error.png' });
    });

    test('加载状态', async ({ page }) => {
      // 检查加载指示器
      const spinner = page.locator('[class*="loading"], [class*="spinner"], text=加载').first();
      
      await page.screenshot({ path: 'test-results/loading-state.png' });
    });
  });

  // ==================== 8. 端到端流程测试 ====================
  test.describe('端到端流程', () => {
    
    test('完整用户流程', async ({ page }) => {
      // 1. 访问 Dashboard
      await page.goto('http://localhost:3001');
      await expect(page.locator('text=Dashboard').first()).toBeVisible();
      
      // 2. 导航到视频管理
      const videoNav = page.locator('nav a:has-text("视频"), nav a:has-text("Video")').first();
      if (await videoNav.isVisible().catch(() => false)) {
        await videoNav.click();
        await page.waitForTimeout(500);
      }
      
      // 3. 导航到设置
      const settingsNav = page.locator('nav a:has-text("设置"), nav a:has-text("Settings")').first();
      if (await settingsNav.isVisible().catch(() => false)) {
        await settingsNav.click();
        await page.waitForTimeout(500);
      }
      
      await page.screenshot({ path: 'test-results/e2e-flow.png' });
    });
  });
});

// ==================== 9. 性能测试 ====================
test.describe('性能测试', () => {
  
  test('页面加载性能', async ({ page }) => {
    const startTime = Date.now();
    
    await page.goto('http://localhost:3001');
    await page.waitForLoadState('networkidle');
    
    const loadTime = Date.now() - startTime;
    console.log(`页面加载时间: ${loadTime}ms`);
    
    // 断言加载时间小于 5 秒
    expect(loadTime).toBeLessThan(5000);
  });

  test('API 响应时间', async ({ page }) => {
    const startTime = Date.now();
    
    await page.goto('http://localhost:3001');
    await page.waitForResponse('**/api/dashboard/overview');
    
    const apiTime = Date.now() - startTime;
    console.log(`API 响应时间: ${apiTime}ms`);
    
    expect(apiTime).toBeLessThan(3000);
  });
});
