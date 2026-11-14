# Security (JWT) — что добавить кроме классов

Классы лежат в:
- `security/SecurityConfig.java`
- `security/JwtService.java`
- `security/JwtAuthenticationFilter.java`
- `security/CustomUserDetailsService.java`
- `controllers/AuthController.java`
- `dto/LoginRequest.java`, `dto/RegisterRequest.java`, `dto/AuthResponse.java`

## 1. Зависимости в pom.xml

Добавь в `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

## 2. Настройки в application.yaml

Добавь блок (секрет — минимум 32 символа, поменяй на свой):

```yaml
app:
  jwt:
    secret: SuperSecretKeyForAkaAdminChangeMe12345
    expiration: 86400000   # 24 часа в мс
```

## 3. Создать администратора

Роль `/admin/**` требует `ROLE_ADMIN`. Проще всего:

1. Зарегистрировать пользователя (пароль сразу хешируется BCrypt):

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@aka.ru","password":"admin123","userPhone":"+70000000000"}'
```

2. Повысить его до администратора в БД:

```bash
docker exec -it akastore psql -U akauser -d akadb -c "UPDATE users SET role='ADMIN' WHERE email='admin@aka.ru';"
```

## 4. Как логиниться

Вход по email **или** телефону — поле `login`:

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin@aka.ru","password":"admin123"}'
```

В ответе `{ "token": "...", "email": "...", "role": "ROLE_ADMIN" }`.
Дальше все запросы к `/admin/**` — с заголовком:

```
Authorization: Bearer <token>
```

## Важно

- **Старые пароли в таблице `users`** должны быть в формате BCrypt. Если раньше они хранились как обычный текст, вход не сработает — перерегистрируй пользователя или перезапиши пароль хешем.
- **Фронтенд перестанет работать без входа**: теперь `/admin/**` отдаёт `401`, пока не передан токен. Нужно добавить экран входа и слать `Authorization: Bearer` в `app.js`. Скажи — сделаю.
- `.cors(Customizer.withDefaults())` в `SecurityConfig` подхватывает твой `WebConfig`. Если фронт запускаешь отдельно (не через Spring) и упрётся в CORS — добавим `CorsConfigurationSource` бин.

## Эндпоинты

| Метод | Путь             | Доступ      |
|-------|------------------|-------------|
| POST  | `/auth/login`    | все         |
| POST  | `/auth/register` | все         |
| *     | `/admin/**`      | ROLE_ADMIN  |
| GET   | `/images/**`, `/`, статика | все |
