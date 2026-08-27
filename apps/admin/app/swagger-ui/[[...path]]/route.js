import { NextResponse } from 'next/server';
import { adminToken, API_BASE, copyResponseHeaders, upstreamHeaders } from '../../../lib/upstream';

export const dynamic = 'force-dynamic';

export async function GET(request, context) {
  const token = adminToken(request);
  if (!token) return NextResponse.json({ message: 'Требуется вход администратора' }, { status: 401 });
  const { path = [] } = await context.params;
  const target = new URL(`${API_BASE}/swagger-ui/${path.length ? path.join('/') : 'index.html'}`);
  target.search = new URL(request.url).search;
  try {
    const upstream = await fetch(target, { headers: upstreamHeaders(request, token), cache: 'no-store' });
    return new NextResponse(upstream.body, { status: upstream.status, headers: copyResponseHeaders(upstream) });
  } catch {
    return NextResponse.json({ message: 'Swagger UI недоступен' }, { status: 502 });
  }
}
