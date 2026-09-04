# ---- сборка: компилируем jar с помощью Maven wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# сначала только файлы, нужные для загрузки зависимостей — кэшируется отдельным слоем,
# пересобирается только при изменении pom.xml, а не при каждой правке кода
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

# ---- запуск: только JRE и готовый jar, без Maven и исходников ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
