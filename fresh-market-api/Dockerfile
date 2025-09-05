FROM openjdk:17-jdk-slim

LABEL maintainer="Fresh Market Team"
LABEL version="1.0.0-SNAPSHOT"
LABEL description="Fresh Market E-commerce API"

# 安装curl用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 创建应用目录
WORKDIR /app

# 复制jar包
COPY target/fresh-market-api-*.jar app.jar

# 创建日志目录
RUN mkdir -p /var/log

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseG1MixedGCCountTarget=16 -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]