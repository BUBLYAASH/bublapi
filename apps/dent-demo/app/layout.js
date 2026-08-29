import { cookies } from 'next/headers';
import Script from 'next/script';
import './styles.css';
import './fixes.css';
import './mobile-header-fix.css';

export default async function RootLayout({ children }) {
  const cookieStore = await cookies();
  const storedTheme = cookieStore.get('bublapi-theme')?.value;
  const theme = storedTheme === 'dark' || storedTheme === 'light' ? storedTheme : undefined;

  return (
    <html lang="ru" data-theme={theme} suppressHydrationWarning>
      <body>
        {children}
        <Script src="/legacy/preload.js" strategy="beforeInteractive" />
        <Script src="/theme.js" strategy="afterInteractive" />
      </body>
    </html>
  );
}
