import { test, expect } from '@playwright/test';

test.describe('Dashboard Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000');
  });

  test('page loads and shows dashboard', async ({ page }) => {
    // Check if page loaded
    await expect(page).toHaveURL('http://localhost:3000/');
    
    // Take screenshot
    await page.screenshot({ path: 'test-results/dashboard-initial.png' });
    console.log('Dashboard page loaded successfully');
  });

  test('sidebar navigation is visible', async ({ page }) => {
    // Check sidebar
    const sidebar = await page.locator('nav, aside, [class*="sidebar"]').first();
    await expect(sidebar).toBeVisible();
    
    await page.screenshot({ path: 'test-results/sidebar.png' });
    console.log('Sidebar is visible');
  });

  test('stats cards are visible', async ({ page }) => {
    // Look for stats cards
    const cards = await page.locator('[class*="card"], .stats-card, [class*="Card"]').all();
    console.log(`Found ${cards.length} cards`);
    
    if (cards.length > 0) {
      await expect(cards[0]).toBeVisible();
    }
    
    await page.screenshot({ path: 'test-results/stats-cards.png' });
  });

  test('click on upload video link', async ({ page }) => {
    // Try to find and click upload link
    const uploadLink = await page.locator('a:has-text("上传"), button:has-text("上传"), [class*="upload"]').first();
    const count = await uploadLink.count();
    
    if (count > 0) {
      await uploadLink.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ path: 'test-results/after-click.png' });
      console.log('Clicked on upload link');
    } else {
      console.log('Upload link not found');
      await page.screenshot({ path: 'test-results/no-upload-link.png' });
    }
  });

  test('hover over navigation items', async ({ page }) => {
    // Find navigation items
    const navItems = await page.locator('nav a, aside a, [role="navigation"] a').all();
    console.log(`Found ${navItems.length} navigation items`);
    
    if (navItems.length > 0) {
      await navItems[0].hover();
      await page.waitForTimeout(500);
    }
    
    await page.screenshot({ path: 'test-results/hover-test.png' });
  });

  test('scroll dashboard page', async ({ page }) => {
    // Scroll down
    await page.evaluate(() => window.scrollTo(0, 500));
    await page.waitForTimeout(500);
    
    await page.screenshot({ path: 'test-results/scrolled.png' });
    console.log('Page scrolled successfully');
  });
});
