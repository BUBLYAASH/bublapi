import crypto from 'node:crypto';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const API_BASE = String(process.env.SPRING_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const API_KEY = process.env.CLINIC_API_KEY || '';
const POLL_MS = Math.max(500, Number(process.env.LIVE_APPOINTMENTS_POLL_MS || 750));

function digest(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function normalizeAppointments(text) {
  try {
    const parsed = JSON.parse(text);
    if (!Array.isArray(parsed)) return text;
    parsed.sort((a,b) => String(a?.id || '').localeCompare(String(b?.id || '')));
    return JSON.stringify(parsed);
  } catch { return text; }
}

export async function GET(request) {
  const authorization = request.headers.get('authorization');
  if (!authorization) return Response.json({ message:'Unauthorized' }, { status:401 });

  const encoder = new TextEncoder();
  let timer = null;
  let heartbeat = null;
  let closed = false;
  let inFlight = false;
  let lastHash = null;

  const stream = new ReadableStream({
    start(controller) {
      const send = (event, data = {}) => {
        if (closed) return;
        controller.enqueue(encoder.encode(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`));
      };
      const close = () => {
        if (closed) return;
        closed = true;
        if (timer) clearInterval(timer);
        if (heartbeat) clearInterval(heartbeat);
        try { controller.close(); } catch {}
      };

      const poll = async () => {
        if (closed || inFlight) return;
        inFlight = true;
        try {
          const headers = { 'Accept':'application/json', 'Authorization':authorization };
          if (API_KEY) headers['X-API-KEY'] = API_KEY;
          const response = await fetch(`${API_BASE}/api/appointments`, { headers, cache:'no-store' });
          if (response.status === 401 || response.status === 403) {
            send('auth-error', { status:response.status });
            close();
            return;
          }
          if (!response.ok) {
            send('upstream-error', { status:response.status });
            return;
          }
          const text = await response.text();
          const currentHash = digest(normalizeAppointments(text));
          if (lastHash === null) {
            lastHash = currentHash;
            send('ready', { pollMs:POLL_MS });
          } else if (currentHash !== lastHash) {
            lastHash = currentHash;
            let count = null;
            try { count = JSON.parse(text).length; } catch {}
            send('appointments-changed', { at:new Date().toISOString(), count });
          }
        } catch (error) {
          send('upstream-error', { message:error instanceof Error ? error.message : 'Unknown error' });
        } finally { inFlight = false; }
      };

      controller.enqueue(encoder.encode(': bublapi-live\n\n'));
      poll();
      timer = setInterval(poll, POLL_MS);
      heartbeat = setInterval(() => {
        if (!closed) controller.enqueue(encoder.encode(': heartbeat\n\n'));
      }, 15000);
      request.signal.addEventListener('abort', close, { once:true });
    },
    cancel() {
      closed = true;
      if (timer) clearInterval(timer);
      if (heartbeat) clearInterval(heartbeat);
    }
  });

  return new Response(stream, { headers:{
    'Content-Type':'text/event-stream; charset=utf-8',
    'Cache-Control':'no-cache, no-store, must-revalidate',
    'Connection':'keep-alive',
    'X-Accel-Buffering':'no'
  }});
}
