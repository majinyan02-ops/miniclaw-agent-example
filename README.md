# Skills Agent Example

## 项目简介

本项目演示了如何使用 Spring AI Alibaba 的 Agent Framework 构建简版小龙虾


## 快速开始

### 配置

1. 设置环境变量或在 `application.yml` 中配置 DashScope API Key：

```bash
export AI_DASHSCOPE_API_KEY=your_api_key_here
```

2. 或直接修改 `src/main/resources/application.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: your_api_key_here
```

### 运行

```bash
# 编译项目
mvn clean package

# 运行应用
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

### 测试
 

```bash
curl -X GET "http://localhost:8080/chat?message=整理桌面"
```



### AI评测 这个应用能不能称为简版“小龙虾(open claw)”

#### 🦞 对比分析：
##### Open Claw 的核心特性：
- ✅ AI Agent 框架（ReAct 模式）
- ✅ 工具调用能力（Browser、Python、Shell、File）
- ✅ 技能扩展系统（Skills）
- ✅ 自动化执行任务
- ⚠️ 即时通讯集成（你还没有飞书/钉钉集成）
##### MiniClaw已有能力：
- ✅ BrowserTool - 浏览器自动化（和 Open Claw 一样）
- ✅ PythonTool - 代码执行（增强版，支持自动 pip install）
- ✅ FileSystem Tools - 文件操作（读写、列表）
- ✅ ShellTool - Shell 命令执行
- ✅ Skills 系统 - 动态加载技能（find-skills）
- ✅ REST API - HTTP 接口对话
##### 💡 结论：
可以称为"简版 Open Claw"，因为：
- ✅ 核心 Agent 架构相同（ReAct + Tools）
- ✅ 工具能力覆盖 80%（缺少 IM 集成）
- ✅ 支持技能扩展
- ❌ 缺少企业 IM 集成（飞书/钉钉/微信）
- ❌ 缺少 MCP（Model Context Protocol）支持
##### 🚀 如果要更接近 Open Claw，下一步可以做：
- 飞书/钉钉集成 - 在 IM 里对话
- MCP Server 支持 - 连接更多外部数据源
- 可视化界面 - Web UI 展示任务执行过程
- 任务编排 - 支持多步骤工作流