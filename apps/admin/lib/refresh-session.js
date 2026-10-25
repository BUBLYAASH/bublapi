import {createHash} from 'node:crypto';
import {NextResponse} from 'next/server';
import {adminPublicOrigin, API_BASE, requestHasSameOrigin} from './upstream';
import {sessionCookieName, sessionCookieOptions} from './session';

export function refreshCookieName() {
    return process.env.ADMIN_COOKIE_SECURE === 'false'
        ? 'bublapi_admin_refresh_dev' : '__Host-bublapi_admin_refresh';
}

export function sessionVersion(token) {
    return token ? createHash('sha256').update(token).digest('hex') : '';
}

export function accessMaxAge(token) {
    try {
        const {exp} = JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString());
        return Math.max(0, Math.floor(exp - Date.now() / 1000));
    } catch {
        return 0;
    }
}

export async function adminAuth(request, action, body) {
    if (!requestHasSameOrigin(request)) {
        return NextResponse.json({message: 'Недопустимый источник запроса'}, {status: 403});
    }
    const headers = new Headers({Accept: 'application/json', Origin: adminPublicOrigin(request)});
    if (body) headers.set('Content-Type', 'application/json');
    const refresh = request.cookies.get(refreshCookieName())?.value;
    if (refresh && action !== 'login') headers.set('Cookie', `admin_refresh_token=${encodeURIComponent(refresh)}`);
    try {
        const upstream = await fetch(`${API_BASE}/api/v1/admin/auth/${action}`, {
            method: 'POST', headers, body, cache: 'no-store', redirect: 'manual'
        });
        const response = NextResponse.json(
            {
                authenticated: upstream.ok && action !== 'logout', ...(!upstream.ok ? {
                    message: 'Ошибка авторизации',
                    status: upstream.status
                } : {})
            },
            {status: upstream.ok ? 200 : upstream.status, headers: {'Cache-Control': 'no-store'}}
        );
        if (!upstream.ok) {
            if (action === 'refresh' && upstream.status === 401) {
                response.cookies.set(sessionCookieName(), '', sessionCookieOptions(0));
                response.cookies.set(refreshCookieName(), '', sessionCookieOptions(0));
            }
            return response;
        }
        if (action === 'logout') {
            response.cookies.set(sessionCookieName(), '', sessionCookieOptions(0));
            response.cookies.set(refreshCookieName(), '', sessionCookieOptions(0));
            return response;
        }
        const payload = await upstream.json();
        const cookie = upstream.headers.getSetCookie().find(value => value.startsWith('admin_refresh_token='));
        const value = cookie?.split(';')[0].slice('admin_refresh_token='.length);
        const maxAge = accessMaxAge(payload.accessToken || '');
        if (!value || !maxAge) return NextResponse.json({message: 'Некорректный ответ авторизации'}, {status: 502});
        response.cookies.set(sessionCookieName(), payload.accessToken, sessionCookieOptions(maxAge));
        // Backend cookie is always HttpOnly, Secure, Lax, 30 days. Map its name/path
        // so it cannot collide with the clinic session on a shared host.
        response.cookies.set(refreshCookieName(), decodeURIComponent(value), sessionCookieOptions(30 * 24 * 60 * 60));
        return response;
    } catch {
        return NextResponse.json({message: 'Сервис авторизации временно недоступен'}, {status: 502});
    }
}
