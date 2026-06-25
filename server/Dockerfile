# ============================================
# AI 生活助手 — Spring Boot Docker 镜像
# ============================================

# 第1阶段：Maven 构建
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -q

# 第2阶段：运行时（轻量 JRE）
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8082

# 启动
ENTRYPOINT ["java", "-jar", "app.jar"]
