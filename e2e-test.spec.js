const { test, expect } = require('@playwright/test');

test.describe('Grace Platform E2E Tests', () => {
  
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000');
  });

  test('Dashboard page loads correctly', async ({ page }) => {
    // Check page title
    await expect(page).toHaveTitle(/Grace/);
    
    // Check if main content is visible
    await expect(page.locator('body')).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/dashboard.png' });
  });

  test('Navigation works - Video Upload page', async ({ page }) => {
    // Click on Video Upload link
    await page.click('text=上传视频');
    
    // Wait for navigation
    await page.waitForURL('**/upload');
    
    // Check if upload page content is visible
    await expect(page.locator('text=上传视频')).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/upload.png' });
  });

  test('Navigation works - Video Management page', async ({ page }) => {
    // Click on Video Management link
    await page.click('text=视频管理');
    
    // Wait for navigation
    await page.waitForURL('**/videos');
    
    // Check if management page content is visible
    await expect(page.locator('text=视频管理')).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/management.png' });
  });

  test('Navigation works - Metadata Review page', async ({ page }) => {
    // Click on Metadata Review link
    await page.click('text=元数据审核');
    
    // Wait for navigation
    await page.waitForURL('**/review');
    
    // Check if review page content is visible
    await expect(page.locator('text=元数据审核')).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/review.png' });
  });

  test('Button interactions - Stats cards clickable', async ({ page }) => {
    // Wait for dashboard to load
    await page.waitForSelector('[data-testid="stats-card"]', { timeout: 5000 });
    
    // Click on first stats card
    await page.click('[data-testid="stats-card"]');
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/stats-clicked.png' });
  });

  test('Form interactions - Date range selector', async ({ page }) => {
    // Find and click date range selector
    await page.click('button:has-text("7天")');
    
    // Wait a moment
    await page.waitForTimeout(500);
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/date-range.png' });
  });

  test('Hover effects - Sidebar navigation', async ({ page }) => {
    // Hover over sidebar items
    const sidebarItems = await page.locator('nav a').all();
    
    for (let i = 0; i < Math.min(sidebarItems.length, 3); i++) {
      await sidebarItems[i].hover();
      await page.waitForTimeout(300);
    }
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/sidebar-hover.png' });
  });

});
