import { sessionCookieName } from './session';

export const API_BASE = String(process.env.SPRING_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const HOP_BY_HOP = new Set(['host', 'cookie', 'content-length', 'connection', 'transfer-encoding', 'keep-alive', 'upgrade', 'proxy-authenticate', 'proxy-authorization', 'te', 'trailers']);

function firstForwardedValue(value) {
  return value?.split(',')[0]?.trim() || '';
}

function normalizedOrigin(value) {
  if (!value) return '';
  try {
    return new URL(value).origin;
  } catch {
    return '';
  }
}

export function requestHasSameOrigin(request) {
  const origin = normalizedOrigin(request.headers.get('origin'));
  if (!origin) return true;

  const allowedOrigins = new Set([new URL(request.url).origin]);
  const configuredOrigin = normalizedOrigin(process.env.ADMIN_PUBLIC_ORIGIN);
  if (configuredOrigin) allowedOrigins.add(configuredOrigin);

  const forwardedHost = firstForwardedValue(request.headers.get('x-forwarded-host'))
    || firstForwardedValue(request.headers.get('host'));
  const forwardedProto = firstForwardedValue(request.headers.get('x-forwarded-proto'));
  if (forwardedHost && (forwardedProto === 'http' || forwardedProto === 'https')) {
    allowedOrigins.add(`${forwardedProto}://${forwardedHost}`);
  }

  return allowedOrigins.has(origin);
}

export function upstreamHeaders(request, token, { contentType } = {}) {
  const headers = new Headers();
  request.headers.forEach((value, name) => {
    if (!HOP_BY_HOP.has(name.toLowerCase())) headers.set(name, value);
  });
  if (token) headers.set('Authorization', token.startsWith('Bearer ') ? token : `Bearer ${token}`);
  if (contentType) headers.set('Content-Type', contentType);
  return headers;
}

export function adminToken(request) {
  return request.cookies.get(sessionCookieName())?.value || '';
}

export function copyResponseHeaders(upstream) {
  const headers = new Headers();
  upstream.headers.forEach((value, name) => {
    const normalized = name.toLowerCase();
    if (!HOP_BY_HOP.has(normalized) && !['set-cookie', 'x-frame-options', 'content-security-policy'].includes(normalized)) {
      headers.set(name, value);
    }
  });
  headers.set('Cache-Control', 'no-store');
  return headers;
}
