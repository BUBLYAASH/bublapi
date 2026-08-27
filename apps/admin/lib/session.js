export const SESSION_MAX_AGE_SECONDS = 8 * 60 * 60;

export function secureCookiesEnabled() {
  return process.env.ADMIN_COOKIE_SECURE !== 'false';
}

export function sessionCookieName() {
  return secureCookiesEnabled()
    ? '__Host-bublapi_admin_session'
    : 'bublapi_admin_session_dev';
}

export function sessionCookieOptions(maxAge = SESSION_MAX_AGE_SECONDS) {
  return {
    httpOnly: true,
    secure: secureCookiesEnabled(),
    sameSite: 'strict',
    path: '/',
    maxAge
  };
}
