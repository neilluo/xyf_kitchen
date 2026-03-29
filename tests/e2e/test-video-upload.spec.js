const { test, expect } = require('@playwright/test');

test.describe('视频上传功能验证', () => {
  test('视频上传初始化 API 正常工作', async ({ page }) => {
    const response = await page.request.post('http://localhost:8080/api/videos/upload/init', {
      data: {
        fileName: 'test-video.mp4',
        fileSize: 1048576,
        format: 'MP4'
      }
    });
    
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.code).toBe(0);
    expect(data.data.uploadId).toBeDefined();
    expect(data.data.totalChunks).toBeGreaterThan(0);
    console.log('✅ 视频上传初始化成功:', data.data.uploadId);
  });

  test('Dashboard 页面加载正常', async ({ page }) => {
    await page.goto('http://localhost:3001');
    await expect(page).toHaveTitle(/Grace/);
    console.log('✅ Dashboard 页面加载成功');
  });
});
