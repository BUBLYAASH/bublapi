import { ensureAdminSession } from './session.js';
async function renew() {
  if (document.hidden) return;
  try { await ensureAdminSession(); }
  catch (error) { if (error.status === 401) location.replace('/login'); }
}
setInterval(renew, 30000);
document.addEventListener('visibilitychange', renew);
window.addEventListener('focus', renew);
void renew();
