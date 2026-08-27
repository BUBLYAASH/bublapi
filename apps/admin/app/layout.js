import { cookies } from 'next/headers';
import Script from 'next/script';
import './styles.css';
import NavigationBridge from './navigation-bridge';

export default async function RootLayout({ children }) {
  const cookieStore = await cookies();
  const storedTheme = cookieStore.get('bublapi-theme')?.value;
  const theme = storedTheme === 'dark' || storedTheme === 'light' ? storedTheme : undefined;

  return (
    <html lang="ru" data-theme={theme} suppressHydrationWarning>
      <body data-surface="admin">
        <NavigationBridge />
        {children}
        <Script src="/theme.js" strategy="afterInteractive" />
      </body>
    </html>
  );
}
