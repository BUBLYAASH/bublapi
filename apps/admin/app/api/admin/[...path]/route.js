import { NextResponse } from 'next/server';
import { adminToken, API_BASE, copyResponseHeaders, requestHasSameOrigin, upstreamHeaders } from '../../../../lib/upstream';
import { sessionCookieName, sessionCookieOptions } from '../../../../lib/session';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

async function proxy(request, context) {
  const token = adminToken(request);
  if (!token) return NextResponse.json({ message: 'Требуется вход администратора' }, { status: 401 });

  if (!['GET', 'HEAD'].includes(request.method) && !requestHasSameOrigin(request)) {
    return NextResponse.json({ message: 'Недопустимый источник запроса' }, { status: 403 });
  }

  const { path = [] } = await context.params;
  const incoming = new URL(request.url);
  const target = new URL(`${API_BASE}/api/admin/${path.join('/')}`);
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

    // Only an authentication failure invalidates the whole browser session.
    // A 403 may be a legitimate per-operation role/permission denial.
    if (upstream.status === 401) {
      response.cookies.set(sessionCookieName(), '', sessionCookieOptions(0));
    }

    return response;
  } catch (error) {
    console.error('Admin API proxy error', { target: target.toString(), error });
    return NextResponse.json({ message: 'Spring Boot API недоступен' }, { status: 502 });
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const HEAD = proxy;
