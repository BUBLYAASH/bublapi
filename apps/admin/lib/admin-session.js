import { API_BASE } from './upstream';

export async function hasValidAdminSession(token) {
  const normalized = String(token || '').trim();
  if (!normalized) return false;

  const authorization = normalized.startsWith('Bearer ')
    ? normalized
    : `Bearer ${normalized}`;

  try {
    const response = await fetch(`${API_BASE}/api/admin/clinics`, {
      method: 'GET',
      headers: {
        Authorization: authorization,
        Accept: 'application/json'
      },
      cache: 'no-store'
    });

    if (response.status === 401 || response.status === 403) return false;

    // A temporary 5xx from Spring is not a reason to destroy an otherwise
    // valid HttpOnly browser session. Protected data calls still fail closed.
    return true;
  } catch (error) {
    console.error('Admin session validation upstream error', error);
    return true;
  }
}
