# 第一階段：編譯
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
COPY . .
# 打包成 jar，跳過測試（測試會在 CI 流程跑）
RUN mvn clean package -DskipTests

# 第二階段：運行環境
FROM mcr.microsoft.com/playwright/java:v1.43.0-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 預設執行環境設定為 prod
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]