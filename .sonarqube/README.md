# SonarQube 配置

## 环境变量配置

在 GitHub Secrets 中添加以下变量：

```
SONAR_TOKEN=<你的 SonarQube token>
SONAR_HOST_URL=https://sonarcloud.io  # 或自托管的 SonarQube 地址
```

## 本地运行 SonarQube 扫描

```bash
# 设置环境变量
export SONAR_TOKEN="your-token"
export SONAR_HOST_URL="https://sonarcloud.io"

# 运行扫描
mvn verify sonar:sonar
```

## SonarQube 质量门禁规则

项目配置了以下质量门禁：

| 指标 | 最低要求 |
|------|----------|
| 代码覆盖率 | ≥ 60% |
| 分支覆盖率 | ≥ 50% |
| 重复行 | ≤ 5% |
| 严重级别 Bug | 0 |
| 主要级别漏洞 | 0 |

## 持续集成中的 SonarQube

GitHub Actions 工作流已配置自动运行 SonarQube 扫描：

1. 在 `test` job 中生成覆盖率报告
2. 在 `code-quality` job 中运行 SonarQube 扫描
3. 扫描结果将自动上传到 SonarQube 服务器

## 自定义质量规则

如需添加自定义质量规则，可以在 SonarQube Web 界面中配置，或创建 `sonar-rules.xml` 文件。
