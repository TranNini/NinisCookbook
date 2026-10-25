FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/NinisCookBook-1.0-SNAPSHOT.jar app.jar

# The database (recipes with their photos, shopping list) and the access code in one volume; the nightly backups in another one
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/cookbook.db \
    COOKBOOK_ACCESS_CODE_FILE=/app/data/access-code.txt \
    COOKBOOK_BACKUP_DIR=/app/backups
RUN mkdir -p /app/data /app/backups
VOLUME /app/data /app/backups

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
