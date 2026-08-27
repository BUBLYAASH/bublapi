import { NextResponse } from 'next/server';
import { adminToken, API_BASE, copyResponseHeaders, upstreamHeaders } from '../../../../lib/upstream';

export const dynamic = 'force-dynamic';

export async function GET(request, context) {
  const token = adminToken(request);
  if (!token) return NextResponse.json({ message: 'Требуется вход администратора' }, { status: 401 });
  const { path = [] } = await context.params;
  const incoming = new URL(request.url);
  const target = new URL(`${API_BASE}/v3/api-docs${path.length ? `/${path.join('/')}` : ''}`);
  target.search = incoming.search;
  try {
    const upstream = await fetch(target, { headers: upstreamHeaders(request, token), cache: 'no-store' });
    return new NextResponse(upstream.body, { status: upstream.status, headers: copyResponseHeaders(upstream) });
  } catch {
    return NextResponse.json({ message: 'OpenAPI недоступен' }, { status: 502 });
  }
}
