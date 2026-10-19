import { adminAuth } from '../../../../../lib/refresh-session';
export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';
export function POST(request) { return adminAuth(request, 'refresh'); }
