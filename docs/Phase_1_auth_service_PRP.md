Phase 1: Auth Service Development - Detailed Breakdown

  Overview

  Build a standalone Spring Boot microservice that handles user authentication, JWT token issuance, and user management.

  ---
  1. Project Setup

  Create new Maven project with dependencies:

  auth-service/
  ├── pom.xml                          # Spring Boot 3.x, Spring Security, JWT libraries
  ├── Dockerfile                       # Multi-stage build
  ├── src/main/java/
  ├── src/main/resources/
  └── src/test/java/

  Key Dependencies:
  ┌────────────────────────────────┬──────────────────────────────┐
  │           Dependency           │           Purpose            │
  ├────────────────────────────────┼──────────────────────────────┤
  │ spring-boot-starter-web        │ REST API                     │
  ├────────────────────────────────┼──────────────────────────────┤
  │ spring-boot-starter-security   │ Authentication/Authorization │
  ├────────────────────────────────┼──────────────────────────────┤
  │ spring-boot-starter-data-jpa   │ Database access              │
  ├────────────────────────────────┼──────────────────────────────┤
  │ spring-boot-starter-validation │ Input validation             │
  ├────────────────────────────────┼──────────────────────────────┤
  │ nimbus-jose-jwt                │ JWT creation with RS256      │
  ├────────────────────────────────┼──────────────────────────────┤
  │ flyway-core                    │ Database migrations          │
  ├────────────────────────────────┼──────────────────────────────┤
  │ postgresql                     │ Database driver              │
  ├────────────────────────────────┼──────────────────────────────┤
  │ spring-boot-starter-actuator   │ Health checks                │
  └────────────────────────────────┴──────────────────────────────┘
  ---
  2. Database Schema & Migrations

  Create Flyway migrations:

  src/main/resources/db/migration/
  ├── V1__create_users_table.sql
  ├── V2__create_roles_table.sql
  ├── V3__create_user_roles_table.sql
  ├── V4__create_refresh_tokens_table.sql
  ├── V5__create_password_reset_tokens_table.sql
  ├── V6__create_rsa_keys_table.sql
  └── V7__insert_default_roles.sql

  Tables to create:
  ┌───────────────────────┬──────────────────────────────────────────────────────────────────────────────────────────┬───────────────────────┐
  │         Table         │                                         Columns                                          │        Purpose        │
  ├───────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────┤
  │ users                 │ id, email, password_hash, first_name, last_name, phone, enabled, locked, failed_attempts │ User accounts         │
  ├───────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────┤
  │ roles                 │ id, name, description                                                                    │ ROLE_USER, ROLE_ADMIN │
  ├───────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────┤
  │ user_roles            │ user_id, role_id                                                                         │ Many-to-many mapping  │
  ├───────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────┤
  │ refresh_tokens        │ id, user_id, token, expires_at, revoked, device_info, ip_address                         │ Refresh token storage │
  ├───────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────┤
  │ password_reset_tokens │ id, user_id, token, expires_at, used                                                     │ Password reset        │
  ├───────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────┤
  │ rsa_keys              │ id (kid), public_key, private_key, active, expires_at                                    │ JWT signing keys      │
  └───────────────────────┴──────────────────────────────────────────────────────────────────────────────────────────┴───────────────────────┘
  ---
  3. Entity Classes

  Create JPA entities:

  // User.java
  @Entity
  @Table(name = "users")
  public class User {
      @Id
      @GeneratedValue(strategy = GenerationType.UUID)
      private UUID id;

      @Column(unique = true, nullable = false)
      private String email;

      @Column(nullable = false)
      private String passwordHash;

      private String firstName;
      private String lastName;
      private String phone;

      private boolean enabled = true;
      private boolean accountLocked = false;
      private int failedLoginAttempts = 0;

      @ManyToMany(fetch = FetchType.EAGER)
      @JoinTable(name = "user_roles", ...)
      private Set<Role> roles;

      private LocalDateTime createdAt;
      private LocalDateTime lastLoginAt;
  }

  // Role.java
  @Entity
  @Table(name = "roles")
  public class Role {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Integer id;

      @Column(unique = true)
      private String name;  // ROLE_USER, ROLE_ADMIN
  }

  // RefreshToken.java
  @Entity
  @Table(name = "refresh_tokens")
  public class RefreshToken {
      @Id
      @GeneratedValue(strategy = GenerationType.UUID)
      private UUID id;

      @ManyToOne
      private User user;

      private String token;
      private LocalDateTime expiresAt;
      private boolean revoked = false;
      private String deviceInfo;
      private String ipAddress;
  }

  // RsaKey.java (for JWT signing)
  @Entity
  @Table(name = "rsa_keys")
  public class RsaKey {
      @Id
      private String id;  // Key ID (kid)

      @Column(columnDefinition = "TEXT")
      private String publicKey;

      @Column(columnDefinition = "TEXT")
      private String privateKey;  // Encrypted

      private boolean active = true;
      private LocalDateTime expiresAt;
  }

  ---
  4. Repository Layer

  // UserRepository.java
  public interface UserRepository extends JpaRepository<User, UUID> {
      Optional<User> findByEmail(String email);
      boolean existsByEmail(String email);
  }

  // RefreshTokenRepository.java
  public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
      Optional<RefreshToken> findByToken(String token);
      void deleteByUser(User user);
      void deleteByExpiresAtBefore(LocalDateTime date);
  }

  // RsaKeyRepository.java
  public interface RsaKeyRepository extends JpaRepository<RsaKey, String> {
      Optional<RsaKey> findByActiveTrue();
      List<RsaKey> findByActiveTrueOrExpiresAtAfter(LocalDateTime date);
  }

  ---
  5. Service Layer

  5.1 JwtService (Core JWT Logic)

  @Service
  public class JwtService {

      // Generate RSA key pair on startup (if none exists)
      public void initializeKeys();

      // Create access token (JWT with RS256)
      public String generateAccessToken(User user);
      // Claims: sub, email, roles, iss, aud, iat, exp, kid

      // Create refresh token (opaque UUID)
      public RefreshToken generateRefreshToken(User user, String deviceInfo, String ip);

      // Validate and parse JWT (used internally, Kong does this externally)
      public Claims validateToken(String token);

      // Get JWKS response (public keys)
      public JwksResponse getJwks();

      // Rotate keys (scheduled task)
      public void rotateKeys();
  }

  5.2 AuthService (Authentication Logic)

  @Service
  public class AuthService {

      // Register new user
      public AuthResponse register(RegisterRequest request);
      // - Validate email not exists
      // - Hash password (BCrypt)
      // - Create user with ROLE_USER
      // - Generate tokens
      // - Return AuthResponse

      // Login user
      public AuthResponse login(LoginRequest request);
      // - Find user by email
      // - Check account not locked
      // - Verify password
      // - Reset failed attempts on success
      // - Increment failed attempts on failure
      // - Lock account if max attempts reached
      // - Generate tokens
      // - Update lastLoginAt

      // Refresh access token
      public TokenResponse refresh(RefreshRequest request);
      // - Find refresh token
      // - Check not expired/revoked
      // - Generate new access token

      // Logout (revoke refresh token)
      public void logout(String refreshToken);

      // Change password
      public void changePassword(UUID userId, ChangePasswordRequest request);
      // - Verify current password
      // - Hash new password
      // - Revoke all refresh tokens
  }

  5.3 UserService (User Management)

  @Service
  public class UserService {

      // Get current user profile
      public UserResponse getCurrentUser(UUID userId);

      // Admin: List all users (paginated)
      public Page<UserResponse> getAllUsers(Pageable pageable);

      // Admin: Get user by ID
      public UserResponse getUserById(UUID id);

      // Admin: Update user
      public UserResponse updateUser(UUID id, UpdateUserRequest request);

      // Admin: Delete user
      public void deleteUser(UUID id);

      // Admin: Update user roles
      public void updateUserRoles(UUID id, List<String> roles);

      // Admin: Enable/disable user
      public void setUserStatus(UUID id, boolean enabled);
  }

  5.4 PasswordResetService

  @Service
  public class PasswordResetService {

      // Request password reset (sends email)
      public void forgotPassword(String email);
      // - Generate reset token
      // - Store with expiration (1 hour)
      // - Send email (or log for now)

      // Reset password with token
      public void resetPassword(ResetPasswordRequest request);
      // - Validate token exists and not expired
      // - Hash new password
      // - Mark token as used
      // - Revoke all refresh tokens
  }

  ---
  6. Controller Layer

  6.1 AuthController

  @RestController
  @RequestMapping("/auth")
  public class AuthController {

      @PostMapping("/register")
      public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request);

      @PostMapping("/login")
      public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request);

      @PostMapping("/refresh")
      public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request);

      @PostMapping("/logout")
      public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token);

      @PostMapping("/forgot-password")
      public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request);

      @PostMapping("/reset-password")
      public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request);

      @PostMapping("/change-password")
      public ResponseEntity<Void> changePassword(@AuthUser UUID userId,
                                                 @Valid @RequestBody ChangePasswordRequest request);

      @GetMapping("/me")
      public ResponseEntity<UserResponse> getCurrentUser(@AuthUser UUID userId);
  }

  6.2 JwksController

  @RestController
  public class JwksController {

      @GetMapping("/.well-known/jwks.json")
      public ResponseEntity<JwksResponse> getJwks();
      // Returns public keys in JWKS format for Kong to fetch
  }

  6.3 UserController (Admin Only)

  @RestController
  @RequestMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public class UserController {

      @GetMapping
      public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable);

      @GetMapping("/{id}")
      public ResponseEntity<UserResponse> getUser(@PathVariable UUID id);

      @PutMapping("/{id}")
      public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateUserRequest request);

      @DeleteMapping("/{id}")
      public ResponseEntity<Void> deleteUser(@PathVariable UUID id);

      @PutMapping("/{id}/roles")
      public ResponseEntity<Void> updateRoles(@PathVariable UUID id,
                                              @RequestBody List<String> roles);

      @PutMapping("/{id}/status")
      public ResponseEntity<Void> updateStatus(@PathVariable UUID id,
                                               @RequestParam boolean enabled);
  }

  ---
  7. Security Configuration

  @Configuration
  @EnableWebSecurity
  @EnableMethodSecurity
  public class SecurityConfig {

      @Bean
      public SecurityFilterChain filterChain(HttpSecurity http) {
          http
              .csrf(csrf -> csrf.disable())
              .cors(cors -> cors.configurationSource(corsConfig()))
              .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
              .authorizeHttpRequests(auth -> auth
                  // Public endpoints
                  .requestMatchers("/auth/register").permitAll()
                  .requestMatchers("/auth/login").permitAll()
                  .requestMatchers("/auth/refresh").permitAll()
                  .requestMatchers("/auth/forgot-password").permitAll()
                  .requestMatchers("/auth/reset-password").permitAll()
                  .requestMatchers("/.well-known/jwks.json").permitAll()
                  .requestMatchers("/actuator/health/**").permitAll()
                  // Admin endpoints
                  .requestMatchers("/users/**").hasRole("ADMIN")
                  // All others require authentication
                  .anyRequest().authenticated()
              )
              .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);

          return http.build();
      }

      @Bean
      public PasswordEncoder passwordEncoder() {
          return new BCryptPasswordEncoder(12);  // Cost factor 12
      }
  }

  ---
  8. DTOs (Request/Response)

  src/main/java/com/realestate/auth/dto/
  ├── request/
  │   ├── RegisterRequest.java      # email, password, firstName, lastName, phone
  │   ├── LoginRequest.java         # email, password
  │   ├── RefreshRequest.java       # refreshToken
  │   ├── ForgotPasswordRequest.java # email
  │   ├── ResetPasswordRequest.java  # token, newPassword
  │   ├── ChangePasswordRequest.java # currentPassword, newPassword
  │   └── UpdateUserRequest.java     # firstName, lastName, phone
  └── response/
      ├── AuthResponse.java          # accessToken, refreshToken, expiresIn, tokenType, user
      ├── TokenResponse.java         # accessToken, expiresIn, tokenType
      ├── UserResponse.java          # id, email, firstName, lastName, roles, createdAt
      ├── JwksResponse.java          # keys[]
      └── ErrorResponse.java         # message, code, timestamp

  ---
  9. Exception Handling

  @RestControllerAdvice
  public class GlobalExceptionHandler {

      @ExceptionHandler(UserNotFoundException.class)
      public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex);

      @ExceptionHandler(EmailAlreadyExistsException.class)
      public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex);

      @ExceptionHandler(InvalidCredentialsException.class)
      public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex);

      @ExceptionHandler(AccountLockedException.class)
      public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex);

      @ExceptionHandler(TokenExpiredException.class)
      public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException ex);

      @ExceptionHandler(MethodArgumentNotValidException.class)
      public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex);
  }

  ---
  10. Configuration Files

  # application.yml
  spring:
    application:
      name: auth-service
    datasource:
      url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:authdb}
      username: ${DB_USER:postgres}
      password: ${DB_PASSWORD:postgres}
    jpa:
      hibernate:
        ddl-auto: validate
    flyway:
      enabled: true

  server:
    port: 8081

  jwt:
    access-token:
      expiration: 900000      # 15 minutes
    refresh-token:
      expiration: 604800000   # 7 days
    issuer: auth-service
    audience: real-estate-api

  security:
    password:
      min-length: 8
    account:
      max-failed-attempts: 5
      lock-duration-minutes: 30

  ---
  11. Testing

  src/test/java/com/realestate/auth/
  ├── controller/
  │   ├── AuthControllerTest.java       # Unit tests
  │   └── AuthControllerIntegrationTest.java
  ├── service/
  │   ├── AuthServiceTest.java
  │   ├── JwtServiceTest.java
  │   └── UserServiceTest.java
  └── repository/
      └── UserRepositoryTest.java

  Test scenarios:
  - Registration with valid/invalid data
  - Login success/failure/account locked
  - Token refresh with valid/expired/revoked token
  - JWKS endpoint returns valid keys
  - Password reset flow
  - Admin user management

  ---
  12. Dockerfile

  # Multi-stage build
  FROM eclipse-temurin:17-jdk-alpine AS build
  WORKDIR /app
  COPY pom.xml .
  COPY src ./src
  RUN ./mvnw clean package -DskipTests

  FROM eclipse-temurin:17-jre-alpine
  RUN addgroup -S spring && adduser -S spring -G spring
  USER spring:spring
  WORKDIR /app
  COPY --from=build /app/target/*.jar app.jar
  EXPOSE 8081
  ENTRYPOINT ["java", "-jar", "app.jar"]

  ---
  13. Helm Chart

  auth-service/helm/auth-service/
  ├── Chart.yaml
  ├── values.yaml
  ├── values-local.yaml
  ├── values-production.yaml
  └── templates/
      ├── deployment.yaml
      ├── service.yaml
      ├── configmap.yaml
      ├── secret.yaml
      └── hpa.yaml

  ---
  Deliverables Summary
  ┌─────────────────────┬───────────────────────────────────────┐
  │        Item         │              Description              │
  ├─────────────────────┼───────────────────────────────────────┤
  │ Spring Boot Project │ Complete auth-service with all layers │
  ├─────────────────────┼───────────────────────────────────────┤
  │ Database Migrations │ 7 Flyway migrations for schema        │
  ├─────────────────────┼───────────────────────────────────────┤
  │ REST API            │ 12 endpoints (auth + user management) │
  ├─────────────────────┼───────────────────────────────────────┤
  │ JWKS Endpoint       │ Public keys for Kong integration      │
  ├─────────────────────┼───────────────────────────────────────┤
  │ JWT with RS256      │ Asymmetric token signing              │
  ├─────────────────────┼───────────────────────────────────────┤
  │ Security Config     │ Spring Security with stateless auth   │
  ├─────────────────────┼───────────────────────────────────────┤
  │ Unit Tests          │ 80%+ code coverage                    │
  ├─────────────────────┼───────────────────────────────────────┤
  │ Integration Tests   │ API endpoint tests                    │
  ├─────────────────────┼───────────────────────────────────────┤
  │ Docker Image        │ Multi-stage build                     │
  ├─────────────────────┼───────────────────────────────────────┤
  │ Helm Chart          │ Kubernetes deployment                 │
  └─────────────────────┴───────────────────────────────────────┘