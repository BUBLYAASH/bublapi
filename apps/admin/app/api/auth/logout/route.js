import { NextResponse } from 'next/server';
import { requestHasSameOrigin } from '../../../../lib/upstream';
import { sessionCookieName, sessionCookieOptions } from '../../../../lib/session';

export async function POST(request) {
  if (!requestHasSameOrigin(request)) {
    return NextResponse.json({ message: 'Недопустимый источник запроса' }, { status: 403 });
  }
  const response = NextResponse.json({ authenticated: false });
  response.cookies.set(sessionCookieName(), '', sessionCookieOptions(0));
  return response;
}
