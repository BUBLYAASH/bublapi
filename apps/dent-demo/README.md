# BublAPI Dent Demo — Next.js SSR

## Что изменено

- Express/static frontend перенесён на Next.js App Router.
- Главная демо-страница и `/admin` теперь приходят как серверный HTML (SSR/пререндеринг оболочки).
- API proxy перенесён в Next Route Handlers и по-прежнему автоматически добавляет `CLINIC_API_KEY`.
- Для записей сотрудников добавлен постоянный SSE-канал `/live/appointments`.
- Пока Spring API не предоставляет native event stream, Next.js проверяет `/api/appointments` раз в `LIVE_APPOINTMENTS_POLL_MS` (по умолчанию 1000 мс) и отправляет браузеру событие только при изменении snapshot.
- Обычный fallback auto-refresh оставлен раз в 15 секунд.
- Добавлены favicon, OpenGraph image, metadata, canonical, robots.txt, sitemap.xml, JSON-LD.
- `/admin`, `/api`, `/live` закрыты от индексации robots; `/admin` имеет `noindex`.
- Runtime-аватары вынесены в volume-friendly каталог `/app/data/uploads/avatars`.

## Почему SSE лучше текущего фронтового polling

Для задачи «сотруднику нужно видеть изменения записей от других сотрудников» направление данных почти всегда server -> browser. Поэтому SSE проще WebSocket: одно постоянное HTTP-соединение, автоматическая потоковая доставка и меньше протокольной сложности.

Текущий вариант — промежуточный: Next.js SSE endpoint сам проверяет Spring API. Самый правильный следующий шаг — сделать native SSE в Spring Boot: после create/change/cancel appointment публиковать доменное событие (у вас уже есть RabbitMQ), а Spring SSE endpoint отправляет событие всем подключениям нужной clinicId. Тогда polling исчезнет полностью и обновления будут реально event-driven.

## Запуск локально

```bash
cp .env.example .env
npm install
npm run dev
```

## Production

```bash
docker build -t dent-demo .
docker run --env-file .env -p 3000:3000 -v bublapi-demo-uploads:/app/data/uploads dent-demo
```

Если Nginx проксирует `demo.dent.bublapi.ru` на `127.0.0.1:3000`, для SSE важно отключить buffering для `/live/`:

```nginx
location /live/ {
    proxy_pass http://127.0.0.1:3000;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 1h;
}
```

Остальные запросы можно проксировать обычным `location /`.

## Важно

`.env` из исходного архива намеренно не включён в результат, чтобы API key не попадал в новый ZIP. Оставь текущий серверный `.env` или создай его из `.env.example`.


## Обновление маршрутизации и тем
Публичные страницы теперь имеют обычные URL: `/`, `/doctors`, `/services`, `/login`. Личные и рабочие разделы также используют path-based маршруты без `#`. Тема по умолчанию берётся из системных настроек и сохраняется в `localStorage` после ручного выбора.
