# LoveSpace Backend

Spring Boot 3.2.x 多模块 Maven 工程（Java 21）。

## 模块

- `lovespace-common`: 公共模块（工具类 / 通用返回体等）
- `lovespace-user`: 用户服务（当前先以此为主）

## 环境要求

- JDK 21
- Maven 3.9+（推荐）

在 Windows 上请确认：

- `JAVA_HOME` 指向 JDK 21
- `java -version` 输出 21
- `mvn -v` 中的 Java 版本也是 21

## 本地构建

```bash
mvn -f lovespace-backend/pom.xml test
```

## 本地运行（用户服务）

```bash
mvn -f lovespace-backend/pom.xml -pl lovespace-user spring-boot:run
```

启动后：

- `GET /health` 返回 `ok`
- Swagger UI: `/swagger-ui.html`

