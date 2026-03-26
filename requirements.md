# 需求文档：视频分发与推广平台

## 简介

本平台为美食博主提供一站式视频分发与推广解决方案。博主可将本地视频上传至平台，由 AI 自动生成视频标题、描述、标签等元数据，经用户审核确认后，通过通用分发接口发布到主流视频平台（MVP 阶段聚焦 YouTube，架构预留抖音、B站、小红书等多平台扩展）。同时，平台集成 OpenCrawl 能力，结合 AI 推广策略建议，自动在社交媒体和论坛进行推广，提高美食视频的曝光度。

技术栈：Java 21 + Spring Boot + DDD，LLM 服务使用阿里云，数据库使用 MySQL。分发层采用策略模式解耦，通过 VideoDistributor 接口 + Registry 模式路由到具体平台实现。

## 术语表

- **Platform（平台）**: 本视频分发与推广系统
- **Video_Manager（视频管理器）**: 负责接收和存储用户上传的本地视频文件的模块
- **Metadata_Generator（元数据生成器）**: 利用阿里云 LLM 服务，基于视频内容自动生成标题、描述、标签等元数据的 AI 模块
- **Review_Console（审核控制台）**: 供用户审核、编辑和确认 AI 生成的视频元数据的界面
- **Video_Distributor（视频分发器）**: 通用视频分发接口，通过策略模式和 Registry 模式路由到具体平台实现
- **YouTube_Distributor（YouTube 分发器）**: VideoDistributor 接口的 YouTube 平台具体实现
- **Channel_Manager（渠道管理器）**: 管理推广渠道配置和推广任务执行的模块
- **Promotion_Advisor（推广顾问）**: 利用 AI 分析视频内容并生成推广策略建议的模块
- **Promotion_Executor（推广执行器）**: 调用 OpenCrawl 能力在社交媒体和论坛自动执行推广任务的模块
- **Distributor_Registry（分发器注册中心）**: 管理和路由所有 VideoDistributor 实现的注册中心
- **PromotionExecutor（推广执行器接口）**: 通用推广执行接口，通过策略模式路由到具体推广渠道实现（如 OpenCrawl）
- **Promotion_Executor_Registry（推广执行注册中心）**: 管理和路由所有 PromotionExecutor 实现的注册中心
- **Domain_Event（领域事件）**: 上下文间通信机制，如 VideoUploadedEvent 触发元数据自动生成
- **Dashboard（仪表盘）**: 展示视频分发与推广整体数据概览的聚合页面
- **Settings_Page（设置页面）**: 管理用户资料、已连接账户、通知偏好和 API Key 的配置页面

## 需求

### 需求 1：本地视频上传

**用户故事：** 作为美食博主，我想要上传本地视频文件到平台，以便后续进行分发和推广。

#### 验收标准

1. WHEN 用户选择本地视频文件并提交上传请求, THE Video_Manager SHALL 接收视频文件并存储到服务器本地文件系统
2. WHILE 视频文件正在上传, THE Video_Manager SHALL 向用户展示上传进度百分比
3. THE Video_Manager SHALL 支持 MP4、MOV、AVI、MKV 格式的视频文件上传
4. THE Video_Manager SHALL 限制单个视频文件大小不超过 5GB
5. IF 上传的文件格式不在支持列表中, THEN THE Video_Manager SHALL 拒绝上传并返回明确的格式错误提示
6. IF 上传过程中网络中断, THEN THE Video_Manager SHALL 保留已上传的部分数据并支持断点续传
7. WHEN 视频上传完成, THE Video_Manager SHALL 生成唯一的视频标识符并记录视频的基本信息（文件名、大小、格式、上传时间）到 MySQL 数据库

### 需求 2：AI 自动生成视频元数据

**用户故事：** 作为美食博主，我想要平台自动为我的视频生成标题、描述和标签，以便节省手动编写元数据的时间。

#### 验收标准

1. WHEN 视频上传完成, THE Metadata_Generator SHALL 自动触发元数据生成流程
2. THE Metadata_Generator SHALL 调用阿里云 LLM 服务，基于视频文件名和用户历史元数据风格生成标题、描述和标签
3. THE Metadata_Generator SHALL 为每个视频生成至少 5 个相关标签
4. THE Metadata_Generator SHALL 生成的标题长度不超过 100 个字符
5. THE Metadata_Generator SHALL 生成的描述长度不超过 5000 个字符
6. IF 阿里云 LLM 服务调用失败, THEN THE Metadata_Generator SHALL 记录错误日志并通知用户手动填写元数据
7. WHEN 元数据生成完成, THE Metadata_Generator SHALL 将生成的元数据与对应视频关联并存储到 MySQL 数据库

### 需求 3：用户审核与确认

**用户故事：** 作为美食博主，我想要在视频发布前审核和编辑 AI 生成的元数据，以便确保内容准确且符合我的风格。

#### 验收标准

1. WHEN 元数据生成完成, THE Review_Console SHALL 展示视频预览和 AI 生成的标题、描述、标签供用户审核
2. THE Review_Console SHALL 允许用户编辑标题、描述和标签的任意字段
3. THE Review_Console SHALL 允许用户添加或删除标签
4. WHEN 用户确认元数据, THE Review_Console SHALL 将视频状态更新为"待发布"并保存最终元数据到 MySQL 数据库
5. THE Review_Console SHALL 允许用户对同一视频多次编辑元数据，直到用户明确确认
6. IF 用户对 AI 生成的元数据不满意, THEN THE Review_Console SHALL 提供"重新生成"按钮触发 Metadata_Generator 重新生成元数据

### 需求 4：YouTube 视频发布

**用户故事：** 作为美食博主，我想要将审核通过的视频一键发布到 YouTube，以便快速触达海外观众。

#### 验收标准

1. WHEN 用户对"待发布"状态的视频点击发布按钮, THE Video_Distributor SHALL 通过 Distributor_Registry 路由到 YouTube_Distributor 执行发布
2. THE YouTube_Distributor SHALL 调用 YouTube Data API v3 上传视频文件并设置标题、描述、标签和隐私状态
3. WHEN 视频发布成功, THE YouTube_Distributor SHALL 将 YouTube 返回的视频 ID 和视频链接存储到 MySQL 数据库，并将视频状态更新为"已发布"
4. IF YouTube API 调用失败, THEN THE YouTube_Distributor SHALL 记录错误详情并将视频状态更新为"发布失败"，同时向用户展示失败原因
5. IF YouTube API 返回配额超限错误, THEN THE YouTube_Distributor SHALL 将发布任务加入重试队列，并在配额恢复后自动重试
6. THE Video_Distributor SHALL 通过统一的 VideoDistributor 接口定义发布方法，使 Controller 层不绑定具体平台实现
7. THE Distributor_Registry SHALL 根据目标平台标识动态路由到对应的 VideoDistributor 实现

### 需求 5：推广渠道管理

**用户故事：** 作为美食博主，我想要管理推广渠道的配置信息，以便控制推广内容的投放范围。

#### 验收标准

1. THE Channel_Manager SHALL 允许用户添加推广渠道，包括渠道名称、渠道类型（社交媒体、论坛、博客）和渠道 URL
2. THE Channel_Manager SHALL 允许用户编辑和删除已有的推广渠道配置
3. THE Channel_Manager SHALL 允许用户启用或禁用单个推广渠道
4. WHILE 推广渠道处于禁用状态, THE Channel_Manager SHALL 跳过该渠道的推广任务执行
5. THE Channel_Manager SHALL 将推广渠道配置信息持久化存储到 MySQL 数据库
6. THE Channel_Manager SHALL 允许用户为推广渠道配置 API Key，API Key 在持久化存储时必须使用 AES-256-GCM 加密，不得以明文形式存储

### 需求 6：AI 推广策略建议

**用户故事：** 作为美食博主，我想要获得 AI 生成的推广策略建议，以便更有效地推广我的美食视频。

#### 验收标准

1. WHEN 用户请求推广策略建议, THE Promotion_Advisor SHALL 调用阿里云 LLM 服务，基于视频元数据和目标推广渠道生成推广文案建议
2. THE Promotion_Advisor SHALL 为每个启用的推广渠道生成定制化的推广文案，包括推广标题和推广正文
3. THE Promotion_Advisor SHALL 根据渠道类型调整推广文案的风格和长度（社交媒体简短精炼，论坛详细深入）
4. WHEN 推广策略生成完成, THE Promotion_Advisor SHALL 将策略建议展示给用户供审核和编辑
5. IF 阿里云 LLM 服务调用失败, THEN THE Promotion_Advisor SHALL 记录错误日志并提示用户手动编写推广文案

### 需求 7：OpenCrawl 自动推广执行

**用户故事：** 作为美食博主，我想要平台自动在社交媒体和论坛发布推广内容，以便提高视频曝光度而无需手动操作。

#### 验收标准

1. WHEN 用户确认推广策略并触发推广执行, THE Promotion_Executor SHALL 调用 OpenCrawl 服务按照推广策略在目标渠道发布推广内容
2. THE Promotion_Executor SHALL 按照推广渠道的优先级顺序依次执行推广任务
3. WHEN 单个渠道的推广任务执行完成, THE Promotion_Executor SHALL 记录执行结果（成功/失败）和渠道返回信息到 MySQL 数据库
4. IF OpenCrawl 服务调用失败, THEN THE Promotion_Executor SHALL 记录错误详情并将该渠道的推广任务标记为"执行失败"
5. IF 单个渠道推广失败, THEN THE Promotion_Executor SHALL 继续执行剩余渠道的推广任务，不中断整体推广流程
6. WHEN 所有渠道的推广任务执行完成, THE Promotion_Executor SHALL 生成推广执行报告，汇总各渠道的执行状态和结果

### 需求 8：系统可扩展性

**用户故事：** 作为平台开发者，我想要系统架构支持便捷地扩展新的视频平台和推广渠道，以便未来快速接入抖音、B站、小红书等平台。

#### 验收标准

1. THE Video_Distributor SHALL 定义统一的分发接口，包含上传视频、查询发布状态、获取视频链接等方法
2. THE Distributor_Registry SHALL 支持通过 Spring Boot 自动装配机制注册新的 VideoDistributor 实现，无需修改已有代码
3. WHEN 新的 VideoDistributor 实现被注册到 Distributor_Registry, THE Distributor_Registry SHALL 自动将其纳入平台路由，使其可通过平台标识被调用
4. THE Platform SHALL 采用 DDD 分层架构，将领域逻辑、应用服务、基础设施和接口层清晰分离
5. THE Platform SHALL 将第三方服务调用（YouTube API、阿里云 LLM、OpenCrawl）封装在基础设施层的适配器中，与领域逻辑解耦
6. THE Promotion_Executor SHALL 定义统一的推广执行接口，通过 Promotion_Executor_Registry 路由到具体推广渠道实现（MVP 阶段为 OpenCrawl），新增推广方式无需修改已有代码
7. THE Platform SHALL 通过领域事件（Domain_Event）实现限界上下文间的通信，如视频上传完成后自动触发元数据生成

### 需求 9：仪表盘数据概览

**用户故事：** 作为美食博主，我想要在登录平台后看到一个数据概览仪表盘，以便快速了解视频分发和推广的整体状况。

#### 验收标准

1. WHEN 用户进入仪表盘页面, THE Dashboard SHALL 展示统计卡片，包括总视频数、待审核数、已发布数和推广中数
2. THE Dashboard SHALL 展示最近上传的视频列表（最多 5 条），包括视频预览缩略图、文件名、上传日期和当前状态
3. THE Dashboard SHALL 展示发布状态分布图（已发布、处理中、失败的数量占比）
4. THE Dashboard SHALL 展示各推广渠道的成功率概览，包括渠道名称、总执行次数、成功次数和成功率百分比
5. THE Dashboard SHALL 展示平台分析数据，包括平均互动率和总曝光量
6. THE Dashboard SHALL 支持按时间范围（近 7 天、近 30 天、近 90 天）筛选推广概览和分析数据
7. THE Dashboard SHALL 通过一次聚合查询获取全部概览数据，减少前端并发请求

### 需求 10：用户设置与账户管理

**用户故事：** 作为美食博主，我想要管理个人资料、连接的第三方平台账户、通知偏好和 API Key，以便个性化配置平台使用体验。

#### 验收标准

1. THE Settings_Page SHALL 允许用户查看和编辑个人资料，包括显示名称和邮箱
2. THE Settings_Page SHALL 允许用户上传和更换头像（支持 JPG/PNG 格式，最大 2MB）
3. THE Settings_Page SHALL 展示已连接的第三方平台账户列表（如 YouTube、Weibo、Bilibili），显示连接状态和账户名
4. THE Settings_Page SHALL 允许用户断开与指定平台的 OAuth 连接，断开后删除存储的 OAuth Token
5. THE Settings_Page SHALL 允许用户配置通知偏好，包括上传完成通知、推广成功通知和系统更新通知的开关
6. THE Settings_Page SHALL 允许用户创建 API Key，创建时显示完整密钥明文（仅此一次），后续仅展示密钥前缀
7. THE Settings_Page SHALL 允许用户查看所有已创建的 API Key 列表，展示用途描述、密钥前缀、过期时间和最后使用时间
8. THE Settings_Page SHALL 允许用户撤销（删除）指定的 API Key
