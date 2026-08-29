import { NextResponse } from 'next/server';
import {
  adminToken,
  API_BASE,
  copyResponseHeaders,
  requestHasSameOrigin,
  upstreamHeaders
} from '../../../../lib/upstream';
import { sessionCookieName, sessionCookieOptions } from '../../../../lib/session';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

/*
 * The legacy admin UI manages users through Spring's /api/users controller.
 * Previously admin.bublapi.ru had no Next route for that path, so Next returned
 * its HTML 404 document. api.js then surfaced that HTML as the error text.
 *
 * Keep the browser talking only to the same-origin admin app and proxy the
 * HttpOnly admin session to Spring as Bearer auth.
 */
async function proxy(request, context) {
  const token = adminToken(request);

  if (!token) {
    return NextResponse.json(
      { message: 'Требуется вход администратора' },
      { status: 401 }
    );
  }

  if (!['GET', 'HEAD'].includes(request.method) && !requestHasSameOrigin(request)) {
    return NextResponse.json(
      { message: 'Недопустимый источник запроса' },
      { status: 403 }
    );
  }

  const { path = [] } = await context.params;
  const incoming = new URL(request.url);
  const suffix = Array.isArray(path) && path.length
    ? `/${path.map(part => encodeURIComponent(part)).join('/')}`
    : '';
  const target = new URL(`${API_BASE}/api/users${suffix}`);
  target.search = incoming.search;

  const init = {
    method: request.method,
    headers: upstreamHeaders(request, token),
    redirect: 'manual',
    cache: 'no-store'
  };

  if (!['GET', 'HEAD'].includes(request.method)) {
    init.body = await request.arrayBuffer();
  }

  try {
    const upstream = await fetch(target, init);
    const response = new NextResponse(upstream.body, {
      status: upstream.status,
      headers: copyResponseHeaders(upstream)
    });

    // Only a genuinely expired/invalid token ends the whole admin session.
    if (upstream.status === 401) {
      response.cookies.set(
        sessionCookieName(),
        '',
        sessionCookieOptions(0)
      );
    }

    return response;
  } catch (error) {
    console.error('Admin users proxy error', {
      target: target.toString(),
      error
    });

    return NextResponse.json(
      { message: 'Spring Boot API недоступен' },
      { status: 502 }
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const HEAD = proxy;
