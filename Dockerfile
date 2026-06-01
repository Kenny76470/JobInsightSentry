# # 第一階段：編譯
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /build

# 💡 優先快取 Maven 依賴層，改程式碼時不用重新下載套件
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 複製原始碼並編譯
COPY src ./src
RUN mvn clean package -DskipTests

# # 第二階段：運行環境
FROM mcr.microsoft.com/playwright/java:v1.43.0-jammy
WORKDIR /app

# 💡 修正路徑：從 /build/target/ 複製打包好的 jar
# 使用萬用字元時，後面目標要明確指定檔名或目錄，這裡建議直接指定為 app.jar
COPY --from=build /build/target/JobInsightSentry-*.jar app.jar

# 預設執行環境設定為 prod
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]