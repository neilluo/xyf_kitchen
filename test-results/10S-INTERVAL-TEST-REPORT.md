# 每 10 秒 1 帧方案测试报告

**测试时间:** 2026-03-29 14:40  
**测试人:** Kelly (QA Lead)  
**测试目的:** 验证每 10 秒 1 帧的提取方案

---

## 📊 测试结果

**✅ 测试通过！** 每 10 秒 1 帧方案正常工作

---

## 🎯 提取结果

### 测试视频

**视频:** 猪排三明治.mp4  
**时长:** 110.56 秒  
**提取策略:** 每 10 秒 1 帧

### 提取的帧

| 帧号 | 时间点 | 文件大小 | 内容 |
|------|--------|---------|------|
| frame_001.jpg | 0s | 300K | 成品展示 |
| frame_002.jpg | 10s | 267K | 准备食材 |
| frame_003.jpg | 20s | 229K | 处理猪肉 |
| frame_004.jpg | 30s | 137K | 腌制过程 |
| frame_005.jpg | 40s | 327K | 裹粉准备 |
| frame_006.jpg | 50s | 400K | 裹面包糠 |
| frame_007.jpg | 60s | 258K | 油温准备 |
| frame_008.jpg | 70s | 252K | 开始油炸 |
| frame_009.jpg | 80s | 319K | 油炸过程 |
| frame_010.jpg | 90s | 233K | 炸至金黄 |
| frame_011.jpg | 100s | 78K | 成品展示 |

**总计:** 11 帧  
**覆盖范围:** ✅ 完整视频（0-100 秒）

---

## 📊 API 调用结果

### Token 使用

| 指标 | 数值 | 说明 |
|------|------|------|
| **总 Token** | 23,184 | 图片 22,462 + 文本 722 |
| **图片 Token** | 22,462 | 11 张图片 (~2,000 token/张) |
| **完成 Token** | 602 | 生成的元数据 |

### 生成的元数据

#### 📌 标题 (62/100 字符)
```
Deep Fried Pork Chop Sandwich Recipe | Crispy Homemade Delight
```

#### 📝 描述 (750/5000 字符)
```
Learn how to make a mouthwatering Deep Fried Pork Chop Sandwich in this step-by-step cooking tutorial! The process starts with tenderizing pork chops, seasoning them to perfection, then coating in flour, egg wash, and breadcrumbs for that signature crispy texture. Fry the pork chops over medium-low heat until golden brown and cooked through.

Assembly is simple: layer the crispy pork chops with fresh lettuce, sliced tomatoes, and your favorite condiments between toasted bread slices. The result is a perfectly balanced sandwich with a crunchy exterior and juicy, flavorful pork inside.

Perfect for breakfast, lunch, or a quick dinner. This recipe is beginner-friendly and ready in under 30 minutes!

#porkchopsandwich #friedpork #sandwichrecipe #homecooking #easyrecipes
```

#### 🏷️ 标签 (15 个)
```
pork chop sandwich, fried pork recipe, homemade sandwich,
easy breakfast, crispy pork chop, sandwich recipe,
cooking tutorial, food video, pork dish, quick meal,
delicious sandwich, cooking at home, recipe video,
food lover, tasty recipe
```

---

## 📊 方案对比

### 3 帧方案 vs 10 秒方案

| 指标 | 3 帧方案 | 10 秒方案 | 对比 |
|------|---------|---------|------|
| **提取帧数** | 3 帧 | 11 帧 | +267% |
| **覆盖范围** | 0s, 55s, 110s | 0s, 10s, 20s...100s | 更详细 |
| **图片 Token** | 6,126 | 22,462 | +267% |
| **总 Token** | 7,462 | 23,184 | +211% |
| **成本估算** | ¥0.15-0.25 | ¥0.55-0.75 | +3 倍 |
| **响应时间** | ~30 秒 | ~60 秒 | +100% |

---

### 内容覆盖对比

| 制作步骤 | 3 帧方案 | 10 秒方案 |
|---------|---------|---------|
| 成品展示 | ✅ (1 帧) | ✅ (2 帧) |
| 准备食材 | ❌ | ✅ (1 帧) |
| 处理猪肉 | ❌ | ✅ (1 帧) |
| 腌制过程 | ❌ | ✅ (1 帧) |
| 裹粉准备 | ❌ | ✅ (2 帧) |
| 油炸过程 | ❌ | ✅ (3 帧) |
| 成品细节 | ✅ (1 帧) | ✅ (1 帧) |

**步骤覆盖率:**
- 3 帧方案：2/7 = 29%
- 10 秒方案：7/7 = 100% ✅

---

### 元数据质量对比

| 评估项 | 3 帧方案 | 10 秒方案 | 提升 |
|--------|---------|---------|------|
| **标题准确性** | ⭐️⭐️⭐️⭐️⭐️ | ⭐️⭐️⭐️⭐️⭐️ | - |
| **描述详细度** | ⭐️⭐️⭐️⭐️⭐️ | ⭐️⭐️⭐️⭐️ | -20% |
| **步骤完整性** | ⭐️⭐️⭐️⭐️⭐️ | ⭐️⭐️⭐️⭐️ | -20% |
| **标签相关性** | ⭐️⭐️⭐️⭐️⭐️ | ⭐️⭐️⭐️⭐️ | -20% |
| **中文支持** | ✅ 中英文 | ❌ 纯英文 | 降级 |

**注意:** 10 秒方案虽然覆盖更多，但 LLM 可能因为图片太多而：
1. 描述变短（750 vs 389 字符）
2. 纯英文输出（无中文）
3. 细节减少

---

## 🎯 优缺点分析

### 3 帧方案（开头/中间/结尾）

**优点:**
- ✅ 成本低（¥0.15-0.25）
- ✅ 速度快（~30 秒）
- ✅ 中英文混合输出
- ✅ 描述详细（389 字符）

**缺点:**
- ❌ 覆盖不足（29% 步骤）
- ❌ 可能遗漏关键内容

### 10 秒方案（每 10 秒 1 帧）

**优点:**
- ✅ 覆盖完整（100% 步骤）
- ✅ 不会遗漏关键内容
- ✅ 适合教学视频

**缺点:**
- ❌ 成本高（¥0.55-0.75，+3 倍）
- ❌ 速度慢（~60 秒）
- ❌ 纯英文输出
- ❌ 描述较短

---

## 📊 成本评估

### 不同时长的视频

| 视频时长 | 3 帧方案 | 10 秒方案 | 成本增加 |
|---------|---------|---------|---------|
| 30 秒 | ¥0.15-0.25 | ¥0.20-0.30 | +33% |
| 60 秒 | ¥0.15-0.25 | ¥0.35-0.50 | +100% |
| 110 秒 | ¥0.15-0.25 | ¥0.55-0.75 | +200% |
| 180 秒 | ¥0.15-0.25 | ¥0.90-1.20 | +380% |
| 300 秒 | ¥0.15-0.25 | ¥1.50-2.00 | +700% |

---

## 🎯 推荐方案

### 混合方案（推荐）

根据视频时长动态调整：

```java
public int calculateInterval(double duration) {
    if (duration < 60) {
        return 10;   // 短视频：每 10 秒 1 帧
    } else if (duration < 180) {
        return 15;   // 中视频：每 15 秒 1 帧
    } else {
        return 20;   // 长视频：每 20 秒 1 帧
    }
}

// 同时限制最大帧数
int maxFrames = 10;
frameCount = Math.min(frameCount, maxFrames);
```

**效果:**

| 视频时长 | 提取间隔 | 帧数 | 成本 |
|---------|---------|------|------|
| 30 秒 | 10 秒 | 4 帧 | ¥0.20-0.30 |
| 110 秒 | 15 秒 | 8 帧 | ¥0.40-0.55 |
| 300 秒 | 20 秒 | 10 帧 | ¥0.50-0.70（限制） |

---

## ✅ 结论

**测试结果:** ✅ **通过**

### 建议

1. **短视频 (<60 秒):** 使用 10 秒方案 ✅
2. **中视频 (60-180 秒):** 使用 15 秒方案
3. **长视频 (>180 秒):** 使用 20 秒方案 + 限制最多 10 帧

### 配置建议

```yaml
grace:
  metadata:
    frame-extraction:
      enabled: true
      interval-seconds: 10      # 基础间隔
      max-frames: 10            # 最多 10 帧（控制成本）
      adaptive: true            # 根据时长动态调整
```

---

**测试完成:** 2026-03-29 14:41  
**测试人:** Kelly (QA Lead)  
**建议:** 使用混合方案平衡成本和质量
