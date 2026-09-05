import Script from 'next/script';
import { cookies } from 'next/headers';
import { notFound, redirect } from 'next/navigation';
import { adminShell } from '../../lib/shells';
import { withThemeToggle } from '../../lib/theme-toggle';
import { sessionCookieName } from '../../lib/session';
import { hasValidAdminSession } from '../../lib/admin-session';
import ThemeToggle from '../theme-toggle';

export const dynamic = 'force-dynamic';

const PAGES = {
  login: { kind: 'login', title: 'Вход администратора', public: true },
  apis: { kind: 'catalog', title: 'Выбор API' },
  'dent/clinics': { view:'admin-clinics', title:'Клиники' },
  'dent/catalog': { view:'admin-catalog', title:'Каталог услуг' },
  'dent/api-keys': { view:'admin-api-keys', title:'API-ключи' },
  'dent/notifications': { view:'admin-notifications', title:'Системные уведомления' },
  'dent/users': { view:'admin-users', title:'Пользователи и роли' },
  'dent/docs': { view:'admin-docs', title:'API-документация' },
  'dent/system': { view:'admin-system', title:'Состояние системы' }
};

const EXTRA_NAV = `
<button class="nav-button hidden" data-admin-only="true" data-view="admin-docs">API-документация</button>
<button class="nav-button hidden" data-admin-only="true" data-view="admin-system">Состояние системы</button>`;

const EXTRA_VIEWS = `
<section class="view hidden" data-admin-only="true" id="view-admin-docs">
  <div class="section-head"><div><h1>API-документация</h1><p class="muted">Swagger UI и защищённая OpenAPI-схема Dent API.</p></div><a class="btn btn-secondary" href="/v3/api-docs" target="_blank" rel="noopener">Открыть JSON</a></div>
  <div class="card docs-frame-card"><iframe class="docs-frame" data-src="/swagger-ui/index.html" title="Swagger UI Dent API"></iframe></div>
</section>
<section class="view hidden" data-admin-only="true" id="view-admin-system">
  <div class="section-head"><div><h1>Состояние системы</h1><p class="muted">Безопасный срез Dent Actuator: только health и info.</p></div><button class="btn btn-primary" id="loadSystemStatus" type="button">Обновить</button></div>
  <div class="grid grid-2"><div class="card"><h2>Health</h2><pre class="output" id="systemHealth">Загрузка…</pre></div><div class="card"><h2>Info</h2><pre class="output" id="systemInfo">Загрузка…</pre></div></div>
</section>`;

async function resolvePage(params) {
  const resolved = await params;
  const key = (resolved?.slug || []).join('/');
  return { key, config: PAGES[key] };
}

export async function generateMetadata({ params }) {
  const { config } = await resolvePage(params);
  if (!config) return {};
  return {
    title: `${config.title} | BublAPI Admin`,
    description: 'Защищённая системная панель управления BublAPI.',
    robots: { index:false, follow:false, nocache:true }
  };
}

function LoginPage() {
  return <>
    <ThemeToggle floating />
    <main className="admin-login-shell">
      <form aria-labelledby="loginTitle" className="admin-login-form" id="adminStandaloneLoginForm" noValidate>
        <h1 id="loginTitle">Вход в BublAPI</h1>
        <label>
          Логин
          <input autoComplete="username" inputMode="email" name="email" placeholder="admin@bublapi.ru" required type="email" />
        </label>
        <label>
          Пароль
          <input autoComplete="current-password" name="password" placeholder="Введите пароль" required type="password" />
        </label>
        <button className="btn btn-primary" type="submit">Войти</button>
        <p aria-live="polite" className="login-error" id="adminLoginError" role="status" />
      </form>
    </main>
    <Script src="/login.js" strategy="afterInteractive" />
  </>;
}

function ApiCatalogSurface() {
  return <>
    <header className="api-portal-header">
      <a className="brand" href="/apis" aria-label="BublAPI Admin — выбор API">
        <span className="brand-mark"><img alt="" src="/favicon.svg" /></span>
        <span>BublAPI<small>общая админ-панель</small></span>
      </a>
      <div className="top-actions">
        <ThemeToggle id="portalThemeToggle" />
        <button className="btn btn-ghost" id="portalLogout" type="button">Выйти</button>
      </div>
    </header>
    <main className="api-catalog-shell">
      <div className="api-catalog-heading">
        <h1>Выберите API</h1>
        <p>Каждый продукт открывается в отдельном рабочем контексте со своей навигацией и инструментами.</p>
      </div>
      <div className="api-product-grid">
        <a className="api-product-card" href="/dent/clinics">
          <div className="api-product-card-head">
            <span className="api-product-mark" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M8.2 3.5c1.4 0 2.4.7 3.8.7s2.4-.7 3.8-.7c2.7 0 4.7 2.2 4.7 5.1 0 2.4-1 4-1.7 6.1-.9 2.8-1.5 5.8-3.4 5.8-1.6 0-1.5-3.7-3.4-3.7s-1.8 3.7-3.4 3.7c-1.9 0-2.5-3-3.4-5.8-.7-2.1-1.7-3.7-1.7-6.1 0-2.9 2-5.1 4.7-5.1Z" /></svg>
            </span>
            <span className="api-status"><span aria-hidden="true" />Доступно</span>
          </div>
          <div>
            <h2>Dent API</h2>
            <p>Клиники, каталог услуг, API-ключи, пользователи, документация и состояние системы.</p>
          </div>
          <span className="api-product-action">Открыть администрирование <svg aria-hidden="true" viewBox="0 0 20 20"><path d="m7 4 6 6-6 6" /></svg></span>
        </a>
      </div>
    </main>
  </>;
}

function buildDentShell(view) {
  let shell = withThemeToggle(adminShell)
    .replace('href="/"', 'href="/apis"')
    .replace('BublAPI Dent<small>системная админ-панель</small>', 'BublAPI Admin<small>Dent API</small>')
    .replace('class="btn btn-ghost hidden" id="adminLogout"', 'class="btn btn-ghost" id="adminLogout"')
    .replace('href="/">Вернуться в клинику', 'href="/apis">К выбору API')
    .replace('<div class="nav-title">Администратор</div>', '<div class="nav-title">Dent API</div>')
    .replace('class="nav-button active" data-view="admin-auth"', 'class="nav-button hidden" data-view="admin-auth"')
    .replace('class="view active" id="view-admin-auth"', 'class="view hidden" id="view-admin-auth"')
    .replace('<button class="nav-button hidden" data-admin-only="true" data-view="admin-users">Пользователи и роли</button>', '<button class="nav-button hidden" data-admin-only="true" data-view="admin-users">Пользователи и роли</button>' + EXTRA_NAV)
    .replace('</main>', EXTRA_VIEWS + '</main>');

  return shell
    .replace(`class="view hidden" data-admin-only="true" id="view-${view}"`, `class="view active" data-admin-only="true" id="view-${view}"`)
    .replace(`class="nav-button hidden" data-admin-only="true" data-view="${view}"`, `class="nav-button active" data-admin-only="true" data-view="${view}"`);
}

export default async function AdminRoute({ params }) {
  const { key, config } = await resolvePage(params);
  if (!config && key) notFound();

  const cookieStore = await cookies();
  const token = cookieStore.get(sessionCookieName())?.value || '';
  const authenticated = await hasValidAdminSession(token);

  if (!key) redirect(authenticated ? '/apis' : '/login');
  if (!config) notFound();

  if (config.public && authenticated) redirect('/apis');
  if (!config.public && !authenticated) redirect('/login');

  if (config.kind === 'login') return <LoginPage />;

  const catalogActive = config.kind === 'catalog';
  const initialView = config.view || 'admin-clinics';

  return <div data-admin-ssr-ready="true" data-admin-role-ready="true">
    <div data-workspace-surface="catalog" hidden={!catalogActive}>
      <ApiCatalogSurface />
    </div>
    <div data-workspace-surface="dent" hidden={catalogActive}>
      <div data-admin-authenticated="true" data-initial-view={initialView} dangerouslySetInnerHTML={{ __html: buildDentShell(initialView) }} />
    </div>
    <Script src="/portal.js" strategy="afterInteractive" />
    <Script src="/legacy/admin.js" type="module" strategy="afterInteractive" />
    <Script src="/workspace-navigation.js" strategy="afterInteractive" />
  </div>;
}
