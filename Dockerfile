# Etapa 1: Compilación del proyecto con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar el archivo de configuración de dependencias
COPY pom.xml .

# Copiar el código fuente y el contenido de resources (incluido el Wallet)
COPY src ./src

# Compilar empaquetando el archivo .jar omitiendo los tests para acelerar el pipeline
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final liviana para ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Copiar la carpeta del Wallet explícitamente para que la app lo encuentre en producción
COPY --from=build /app/src/main/resources/wallet /app/src/main/resources/wallet

# Exponer el puerto solicitado en el requerimiento
EXPOSE 8080

# Comando para ejecutar el microservicio
ENTRYPOINT ["java", "-jar", "app.jar"]