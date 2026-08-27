export const dynamic = 'force-dynamic';
export function GET() {
  return Response.json({ apiUrl: process.env.SPRING_API_URL || 'http://localhost:8080', apiKeyConfigured: Boolean(process.env.CLINIC_API_KEY) }, { headers: { 'Cache-Control': 'no-store' } });
}
