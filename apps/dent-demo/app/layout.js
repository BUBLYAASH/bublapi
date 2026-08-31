import { cookies } from 'next/headers';
import Script from 'next/script';
import './styles.css';
import './fixes.css';
import './final-fixes.css';

export default async function RootLayout({ children }) {
  const cookieStore = await cookies();
  const storedTheme = cookieStore.get('bublapi-theme')?.value;
  const theme = storedTheme === 'dark' || storedTheme === 'light' ? storedTheme : undefined;

  return (
    <html lang="ru" data-theme={theme} suppressHydrationWarning>
      <body>
        {children}
        <Script src="/legacy/preload.js" strategy="beforeInteractive" />
        <Script src="/legacy/stability-guard.js" strategy="beforeInteractive" />

        {/*
          These scripts mutate body / application DOM. Running them before
          React hydration changes SSR attributes (notably data-sidebar-mode)
          before React compares the server markup with the client tree and
          causes the hydration warning.
        */}
        <Script src="/legacy/final-fixes.js" strategy="afterInteractive" />
        <Script src="/legacy/calendar-prefill-fix.js" strategy="afterInteractive" />

        <Script src="/theme.js" strategy="afterInteractive" />
      </body>
    </html>
  );
}
