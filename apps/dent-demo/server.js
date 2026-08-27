import 'dotenv/config';
import express from 'express';
import path from 'node:path';
import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {closeLogs, logger, redact, requestId} from './logger.js';

const app = express();
const port = Number(process.env.PORT || 3000);
const apiBaseUrl = String(
    process.env.SPRING_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const clinicApiKey = process.env.CLINIC_API_KEY || '';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

app.disable('x-powered-by');
app.use(express.json({limit: '7mb'}));
app.use(express.urlencoded({extended: true}));

app.use((req, res, next) => {
  const startedAt = process.hrtime.bigint();
  const id = requestId(req);
  req.requestId = id;
  res.setHeader('X-Request-ID', id);

  res.on('finish', () => {
    const durationMs = Number(process.hrtime.bigint() - startedAt) / 1_000_000;
    logger.access('HTTP request completed', {
      requestId: id,
      method: req.method,
      path: req.originalUrl,
      status: res.statusCode,
      durationMs: Number(durationMs.toFixed(2)),
      ip: req.ip,
      userAgent: req.get('user-agent') || null
    });
  });

  next();
});

const avatarDirectory = path.join(__dirname, 'public', 'uploads', 'avatars');
await fs.mkdir(avatarDirectory, {recursive: true});

app.post('/demo/avatar-upload', async (req, res) => {
  try {
    const dataUrl = String(req.body?.dataUrl || '');
    const match = dataUrl.match(
        /^data:(image\/(?:png|jpeg|webp));base64,([A-Za-z0-9+/=]+)$/);
    if (!match) {
      return res.status(400).json({message: 'Некорректный формат изображения'});
    }
    const extensions = {
      'image/png': 'png', 'image/jpeg': 'jpg', 'image/webp': 'webp'
    };
    const buffer = Buffer.from(match[2], 'base64');
    if (buffer.length > 5 * 1024 * 1024) {
      return res.status(413).json(
          {message: 'Размер аватара не должен превышать 5 МБ'});
    }
    const filename = `${crypto.randomUUID()}.${extensions[match[1]]}`;
    await fs.writeFile(path.join(avatarDirectory, filename), buffer,
        {flag: 'wx'});
    res.status(201).json({avatarUrl: `/uploads/avatars/${filename}`});
  } catch (error) {
    logger.error('Avatar upload failed', {requestId: req.requestId, error});
    res.status(500).json({message: 'Не удалось сохранить аватар'});
  }
});

app.get('/demo-config', (_req, res) => {
  res.json({
    apiUrl: apiBaseUrl, apiKeyConfigured: Boolean(clinicApiKey)
  });
});

app.use('/api', async (req, res) => {
  const proxyStartedAt = process.hrtime.bigint();
  const targetUrl = `${apiBaseUrl}${req.originalUrl}`;
  const headers = new Headers();

  for (const [name, value] of Object.entries(req.headers)) {
    if (!value) {
      continue;
    }
    if (['host', 'content-length', 'connection'].includes(name.toLowerCase())) {
      continue;
    }
    headers.set(name, Array.isArray(value) ? value.join(',') : value);
  }

  if (clinicApiKey) {
    headers.set('X-API-Key', clinicApiKey);
  }

  let body;
  if (!['GET', 'HEAD'].includes(req.method)) {
    if (req.body !== undefined && Object.keys(req.body ?? {}).length > 0) {
      body = JSON.stringify(req.body);
      headers.set('Content-Type', 'application/json');
    }
  }

  try {
    logger.proxy('Proxy request started', {
      requestId: req.requestId,
      method: req.method,
      path: req.originalUrl,
      targetUrl,
      body: redact(req.body ?? null)
    });

    const apiResponse = await fetch(targetUrl, {
      method: req.method, headers, body, redirect: 'manual'
    });

    const contentType = apiResponse.headers.get('content-type') || '';
    const responseText = await apiResponse.text();
    const durationMs = Number(process.hrtime.bigint() - proxyStartedAt)
        / 1_000_000;

    logger.proxy('Proxy response received', {
      requestId: req.requestId,
      method: req.method,
      path: req.originalUrl,
      status: apiResponse.status,
      durationMs: Number(durationMs.toFixed(2)),
      contentType,
      responseBytes: Buffer.byteLength(responseText)
    });

    res.status(apiResponse.status);
    if (contentType) {
      res.setHeader('Content-Type', contentType);
    }

    if (!responseText) {
      res.end();
      return;
    }

    res.send(responseText);
  } catch (error) {
    const durationMs = Number(process.hrtime.bigint() - proxyStartedAt)
        / 1_000_000;
    logger.proxyError('API proxy failed', {
      requestId: req.requestId,
      method: req.method,
      path: req.originalUrl,
      targetUrl,
      durationMs: Number(durationMs.toFixed(2)),
      error
    });
    res.status(502).json({
      message: 'Spring Boot API is unavailable',
      details: error instanceof Error ? error.message : String(error)
    });
  }
});

app.use(express.static(path.join(__dirname, 'public'), {
  etag: false, lastModified: false, setHeaders(res, filePath) {
    if (/\.(?:html|js|css)$/i.test(filePath)) {
      res.setHeader('Cache-Control',
          'no-store, no-cache, must-revalidate, proxy-revalidate');
      res.setHeader('Pragma', 'no-cache');
      res.setHeader('Expires', '0');
    }
  }
}));

app.get('/admin', (_req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'admin.html'));
});

app.get('*', (_req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

const server = app.listen(port, () => {
  logger.info('BublAPI Dent demo started', {
    port,
    demoUrl: `http://localhost:${port}`,
    adminUrl: `http://localhost:${port}/admin`,
    springApiUrl: apiBaseUrl,
    clinicApiKeyConfigured: Boolean(clinicApiKey),
    nodeVersion: process.version,
    pid: process.pid
  });

  if (!clinicApiKey) {
    logger.warn(
        'CLINIC_API_KEY is not configured. Clinic endpoints may return 401.');
  }
});

process.on('uncaughtException', error => {
  logger.error('Uncaught exception', {error});
});

process.on('unhandledRejection', reason => {
  logger.error('Unhandled promise rejection',
      {error: reason instanceof Error ? reason : String(reason)});
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    logger.info('Shutdown signal received', {signal});
    server.close(() => {
      logger.info('HTTP server stopped', {signal});
      closeLogs();
      process.exit(0);
    });
  });
}
