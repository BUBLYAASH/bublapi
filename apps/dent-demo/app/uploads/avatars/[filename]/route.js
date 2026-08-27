import fs from 'node:fs/promises';
import path from 'node:path';

export const runtime = 'nodejs';
const ROOT = process.env.AVATAR_STORAGE_PATH || '/app/data/uploads/avatars';
const TYPES = { '.png':'image/png', '.jpg':'image/jpeg', '.jpeg':'image/jpeg', '.webp':'image/webp' };

export async function GET(_request, context) {
  const { filename } = await context.params;
  if (!/^[a-zA-Z0-9._-]+$/.test(filename)) return new Response(null,{status:404});
  try {
    const data = await fs.readFile(path.join(ROOT, filename));
    return new Response(data, { headers:{ 'Content-Type': TYPES[path.extname(filename).toLowerCase()] || 'application/octet-stream', 'Cache-Control':'public, max-age=2592000, immutable', 'X-Content-Type-Options':'nosniff' } });
  } catch { return new Response(null,{status:404}); }
}
