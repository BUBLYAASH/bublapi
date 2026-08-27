const lastModified = new Date('2026-08-10T00:00:00+03:00');
export default function sitemap() {
  return [
    { url:'https://demo.dent.bublapi.ru/', lastModified, changeFrequency:'weekly', priority:1 },
    { url:'https://demo.dent.bublapi.ru/doctors', lastModified, changeFrequency:'weekly', priority:0.9 },
    { url:'https://demo.dent.bublapi.ru/services', lastModified, changeFrequency:'weekly', priority:0.9 }
  ];
}
