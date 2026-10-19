import { NextResponse } from 'next/server';
import { adminAuth } from '../../../../../lib/refresh-session';
export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';
export async function POST(request) {
  let credentials;
  try { credentials = await request.json(); }
  catch { return NextResponse.json({ message: 'Некорректный формат данных' }, { status: 400 }); }
  const email = String(credentials?.email || '').trim().toLowerCase();
  const password = String(credentials?.password || '');
  if (!email || !password) return NextResponse.json({ message: 'Введите логин и пароль' }, { status: 400 });
  return adminAuth(request, 'login', JSON.stringify({ email, password }));
}
