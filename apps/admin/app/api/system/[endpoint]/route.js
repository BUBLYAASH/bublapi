import { NextResponse } from 'next/server';
import { adminToken, API_BASE, copyResponseHeaders, upstreamHeaders } from '../../../../lib/upstream';

const ALLOWED = new Set(['health', 'info']);
export const dynamic = 'force-dynamic';

export async function GET(request, context) {
  const token = adminToken(request);
  if (!token) return NextResponse.json({ message: 'Требуется вход администратора' }, { status: 401 });
  const { endpoint } = await context.params;
  if (!ALLOWED.has(endpoint)) return NextResponse.json({ message: 'Actuator endpoint запрещён' }, { status: 404 });
  try {
    const upstream = await fetch(`${API_BASE}/actuator/${endpoint}`, { headers: upstreamHeaders(request, token), cache: 'no-store' });
    return new NextResponse(upstream.body, { status: upstream.status, headers: copyResponseHeaders(upstream) });
  } catch {
    return NextResponse.json({ message: 'Диагностика недоступна' }, { status: 502 });
  }
}
