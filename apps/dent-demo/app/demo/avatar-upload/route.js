import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';

export const runtime = 'nodejs';
const ROOT = process.env.AVATAR_STORAGE_PATH || '/app/data/uploads/avatars';
const MIME_EXT = { 'image/png':'png', 'image/jpeg':'jpg', 'image/webp':'webp' };

export async function POST(request) {
  try {
    const payload = await request.json();
    const match = String(payload?.dataUrl || '').match(/^data:(image\/(?:png|jpeg|webp));base64,([A-Za-z0-9+/=]+)$/);
    if (!match) return Response.json({ message:'Некорректный формат изображения' }, { status:400 });
    const buffer = Buffer.from(match[2], 'base64');
    if (buffer.length > 5 * 1024 * 1024) return Response.json({ message:'Размер аватара не должен превышать 5 МБ' }, { status:413 });
    await fs.mkdir(ROOT, { recursive:true });
    const filename = `${crypto.randomUUID()}.${MIME_EXT[match[1]]}`;
    await fs.writeFile(path.join(ROOT, filename), buffer, { flag:'wx' });
    return Response.json({ avatarUrl:`/uploads/avatars/${filename}` }, { status:201 });
  } catch (error) {
    console.error('Avatar upload failed', error);
    return Response.json({ message:'Не удалось сохранить аватар' }, { status:500 });
  }
}
