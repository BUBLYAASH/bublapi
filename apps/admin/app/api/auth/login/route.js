import { NextResponse } from 'next/server';
import { API_BASE, requestHasSameOrigin } from '../../../../lib/upstream';
import { sessionCookieName, sessionCookieOptions } from '../../../../lib/session';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST(request) {
  if (!requestHasSameOrigin(request)) {
    return NextResponse.json({ message: 'Недопустимый источник запроса' }, { status: 403 });
  }

  let credentials;
  try {
    credentials = await request.json();
  } catch {
    return NextResponse.json({ message: 'Некорректный формат данных' }, { status: 400 });
  }

  const email = String(credentials?.email || '').trim().toLowerCase();
  const password = String(credentials?.password || '');
  if (!email || !password) {
    return NextResponse.json({ message: 'Введите логин и пароль' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${API_BASE}/api/admin/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ email, password }),
      cache: 'no-store'
    });

    if (!upstream.ok) {
      return NextResponse.json(
        { message: upstream.status === 429 ? 'Слишком много попыток. Попробуйте позже.' : 'Неверный логин или пароль' },
        { status: upstream.status === 429 ? 429 : 401 }
      );
    }

    const payload = await upstream.json();
    if (!payload?.token) return NextResponse.json({ message: 'Backend не вернул сессию' }, { status: 502 });

    const response = NextResponse.json({ authenticated: true });
    response.cookies.set(sessionCookieName(), payload.token, sessionCookieOptions());
    return response;
  } catch (error) {
    console.error('Admin login upstream error', error);
    return NextResponse.json({ message: 'Сервис авторизации временно недоступен' }, { status: 502 });
  }
}
