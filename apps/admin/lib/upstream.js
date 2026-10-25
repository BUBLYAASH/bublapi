import {sessionCookieName} from './session';

export const API_BASE = String(process.env.SPRING_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const HOP_BY_HOP = new Set([
    'host',
    'cookie',
    'content-length',
    'connection',
    'transfer-encoding',
    'keep-alive',
    'upgrade',
    'proxy-authenticate',
    'proxy-authorization',
    'te',
    'trailers',
    'origin',
    'authorization',
    'x-api-key'
]);

export function adminPublicOrigin(request) {
    return process.env.ADMIN_PUBLIC_ORIGIN
        ? new URL(process.env.ADMIN_PUBLIC_ORIGIN).origin
        : new URL(request.url).origin;
}

export function requestHasSameOrigin(request) {
    const origin = request.headers.get('origin');
    return !!origin && origin !== 'null' && origin === adminPublicOrigin(request);
}

export function upstreamHeaders(request, token, {contentType} = {}) {
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
        if (!HOP_BY_HOP.has(normalized) && !['set-cookie', 'x-frame-options', 'content-security-policy', 'content-encoding', 'content-length'].includes(normalized)) {
            headers.set(name, value);
        }
    });
    headers.set('Cache-Control', 'no-store');
    return headers;
}
