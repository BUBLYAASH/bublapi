# BublAPI Admin

Отдельная системная панель для `admin.bublapi.ru`.

## Локальный запуск

```bash
cp .env.example .env.local
npm install
npm run dev
```

Для локального HTTP установите `ADMIN_COOKIE_SECURE=false`. В production не задавайте это значение: по умолчанию используется cookie `__Host-bublapi_admin_session` с `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/` и сроком восемь часов.

## Переменные окружения

- `SPRING_API_URL` — внутренний адрес Spring Boot API, например `http://bublapi-api:8080`.
- `ADMIN_PUBLIC_ORIGIN` — публичный origin панели без завершающего `/`, например `https://admin.bublapi.ru`.
- `ADMIN_COOKIE_SECURE` — только локальная возможность отключить Secure; в production должна отсутствовать или быть `true`.

JWT никогда не передаётся браузерному JavaScript. Next.js хранит его в HttpOnly cookie и добавляет `Authorization` при серверном проксировании.

HTTP API панели доступен под префиксом `/api/v1/`: авторизация — `/api/v1/auth/`, административные запросы — `/api/v1/admin/`, диагностика — `/api/v1/system/`. Запросы к Spring Boot также используют `/api/v1/`; `SPRING_API_URL` задаётся без этого префикса.

## Доступные системные поверхности

- `/docs` — Swagger UI;
- `/v3/api-docs` — OpenAPI JSON;
- `/system` — безопасный срез Actuator (`health`, `info`).

Все эти маршруты требуют активную admin-сессию. Полные Actuator endpoint намеренно не проксируются.
