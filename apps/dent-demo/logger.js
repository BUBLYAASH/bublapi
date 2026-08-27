import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

const LOG_DIR = path.resolve(process.env.LOG_DIR || './logs');
const MAX_LOG_SIZE_BYTES = Number(process.env.LOG_MAX_SIZE_MB || 10) * 1024 * 1024;
const MAX_ARCHIVES = Math.max(1, Number(process.env.LOG_MAX_ARCHIVES || 5));

fs.mkdirSync(LOG_DIR, { recursive: true });

const streams = new Map();

function getLogPath(channel) {
  return path.join(LOG_DIR, `${channel}.log`);
}

function rotateIfNeeded(channel) {
  const filePath = getLogPath(channel);
  try {
    const stat = fs.statSync(filePath);
    if (stat.size < MAX_LOG_SIZE_BYTES) return;
  } catch {
    return;
  }

  const oldStream = streams.get(channel);
  if (oldStream) {
    oldStream.end();
    streams.delete(channel);
  }

  for (let index = MAX_ARCHIVES - 1; index >= 1; index -= 1) {
    const from = `${filePath}.${index}`;
    const to = `${filePath}.${index + 1}`;
    if (fs.existsSync(from)) fs.renameSync(from, to);
  }

  fs.renameSync(filePath, `${filePath}.1`);
}

function streamFor(channel) {
  rotateIfNeeded(channel);
  if (!streams.has(channel)) {
    streams.set(channel, fs.createWriteStream(getLogPath(channel), { flags: 'a' }));
  }
  return streams.get(channel);
}

function safeError(error) {
  if (!(error instanceof Error)) return String(error);
  return {
    name: error.name,
    message: error.message,
    stack: error.stack
  };
}

function normalizeMeta(meta = {}) {
  const result = {};
  for (const [key, value] of Object.entries(meta)) {
    result[key] = value instanceof Error ? safeError(value) : value;
  }
  return result;
}

export function writeLog(channel, level, message, meta = {}) {
  const entry = {
    timestamp: new Date().toISOString(),
    level,
    message,
    ...normalizeMeta(meta)
  };
  const line = `${JSON.stringify(entry)}\n`;
  streamFor(channel).write(line);

  const consoleMethod = level === 'error' ? console.error : level === 'warn' ? console.warn : console.log;
  consoleMethod(`[${entry.timestamp}] [${level.toUpperCase()}] ${message}`, meta);
}

export const logger = {
  info(message, meta) { writeLog('app', 'info', message, meta); },
  warn(message, meta) { writeLog('app', 'warn', message, meta); },
  error(message, meta) {
    writeLog('app', 'error', message, meta);
    writeLog('error', 'error', message, meta);
  },
  access(message, meta) { writeLog('access', 'info', message, meta); },
  proxy(message, meta) { writeLog('proxy', 'info', message, meta); },
  proxyError(message, meta) {
    writeLog('proxy', 'error', message, meta);
    writeLog('error', 'error', message, meta);
  }
};

export function requestId(req) {
  return String(req.headers['x-request-id'] || crypto.randomUUID());
}

const SECRET_KEYS = /password|token|authorization|cookie|api[-_]?key|secret|dataurl/i;

export function redact(value, depth = 0) {
  if (depth > 5) return '[TRUNCATED]';
  if (Array.isArray(value)) return value.map(item => redact(item, depth + 1));
  if (!value || typeof value !== 'object') return value;

  const output = {};
  for (const [key, item] of Object.entries(value)) {
    output[key] = SECRET_KEYS.test(key) ? '[REDACTED]' : redact(item, depth + 1);
  }
  return output;
}

export function closeLogs() {
  for (const stream of streams.values()) stream.end();
  streams.clear();
}
