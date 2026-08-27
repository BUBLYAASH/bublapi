const isDevelopment = process.env.NODE_ENV === 'development';
const contentSecurityPolicy = "default-src 'self'; script-src 'self' 'unsafe-inline'" 
  + (isDevelopment ? " 'unsafe-eval'" : '')
  + "; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-src 'self'; frame-ancestors 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'";

const nextConfig = {
  output: 'standalone',
  poweredByHeader: false,
  compress: true,
  turbopack: { root: import.meta.dirname },
  async headers() {
    return [{
      source: '/:path*',
      headers: [
        { key: 'X-Content-Type-Options', value: 'nosniff' },
        { key: 'X-Frame-Options', value: 'SAMEORIGIN' },
        { key: 'Content-Security-Policy', value: contentSecurityPolicy },
        { key: 'Referrer-Policy', value: 'no-referrer' },
        { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
        { key: 'Strict-Transport-Security', value: 'max-age=31536000; includeSubDomains' },
        { key: 'Cache-Control', value: 'no-store' }
      ]
    }];
  }
};

export default nextConfig;
