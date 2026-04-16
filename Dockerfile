# LoveSpace 后端（多模块工程请在本地执行 Maven 打包后再构建镜像）
# 推荐：在 lovespace-backend 父目录执行
#   mvn -pl lovespace-user -am package -DskipTests -Plovespace-rag
# 将 lovespace-user/target/lovespace-user-*.jar 复制到本目录 target/（目录内仅保留一个可执行 jar）
# 或将 fat jar 重命名为 app.jar 放在与本 Dockerfile 同级目录。
#
# 构建（在 docker-compose.yml 所在目录）：
#   docker compose build backend

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /build
# 构建上下文需包含 app.jar 或 target/*.jar（见 .dockerignore）
COPY . /build/
RUN set -e; \
    if [ -f /build/app.jar ]; then \
      cp /build/app.jar /application.jar; \
    elif ls /build/target/*.jar >/dev/null 2>&1; then \
      cp /build/target/*.jar /application.jar; \
    else \
      echo "ERROR: 请先将可执行 jar 放到 lovespace-backend/app.jar 或 lovespace-backend/target/（仅一个 jar）"; \
      exit 1; \
    fi && \
    rm -rf /build

WORKDIR /app
RUN mv /application.jar /app/application.jar

# 默认以 root 运行，便于挂载 ./data/uploads 写入；若需降权可自行增加 USER 与卷权限
EXPOSE 8081

# 堆内存等可通过 JAVA_TOOL_OPTIONS 或 compose 环境变量注入
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application.jar"]
