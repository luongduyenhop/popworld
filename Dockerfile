# ==============================================================================
# POPWORLD PLATFORM - MULTI-STAGE DOCKERFILE
# Stage 1: Build Artifact JAR (Java 21 JDK)
# Stage 2: Production Runtime Container (Java 21 JRE, Non-Root User)
# ==============================================================================

# ------------------------------------------------------------------------------
# STAGE 1: BUILDER
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Sao chép Maven wrapper và file cấu hình dependencies trước để tận dụng Docker cache layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Cấp quyền thực thi cho script Maven wrapper
RUN chmod +x ./mvnw

# Tải trước dependencies (nếu có thể)
RUN ./mvnw dependency:go-offline -B || true

# Sao chép toàn bộ mã nguồn vào image
COPY src/ ./src/

# Biên dịch và đóng gói runnable Fat JAR (bỏ qua tests vì đã test qua CI/local)
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# STAGE 2: PRODUCTION RUNTIME
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runner

LABEL maintainer="PopWorld DevOps Team <devops@popworld.com>"
LABEL description="PopWorld Art Toy & Blind Box E-Commerce Platform"

WORKDIR /app

# Cài đặt curl phục vụ container healthcheck và dọn dẹp cache apt
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Tạo group và user non-root để tăng cường an ninh container (Nguyên tắc Least Privilege)
RUN groupadd -g 10001 popworld \
    && useradd -u 10001 -g popworld -s /bin/sh -m popworld

# Sao chép file JAR từ stage builder và gán quyền cho user popworld
COPY --from=builder --chown=popworld:popworld /build/target/*.jar app.jar

# Chuyển sang chạy với user không đặc quyền
USER popworld

# Khai báo cổng ứng dụng lắng nghe
EXPOSE 8080

# Cấu hình biến môi trường mặc định
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

# Chỉ thị kiểm tra sức khỏe container tự động
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# Cấu hình khởi động tối ưu JVM trong môi trường container
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Ho_Chi_Minh", \
    "-jar", "app.jar"]
