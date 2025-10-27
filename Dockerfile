# Etapa 1: Build (usar imagen basada en Debian para compatibilidad con protoc)
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .

# Descargar dependencias (cache layer)
RUN mvn dependency:go-offline -B

# Copiar código fuente y proto files
COPY src ./src

# Compilar la aplicación (incluyendo generación de proto)
# protoc-gen-grpc-java requiere glibc, que está disponible en imágenes Debian
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Instalar herramientas de red 
RUN apk add --no-cache curl wget

# Copiar el JAR compilado desde la etapa de build
COPY --from=build /app/target/route-processing-service-*.jar /app/route-processing-service.jar

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app

USER spring:spring

# Exponer el puerto de la aplicación
EXPOSE 8086

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/route-processing-service.jar"]
