export default function handler(req, res) {
  const key = process.env.GEOAPIFY_API_KEY;
  if (!key) return res.status(500).json({ ok: false, error: 'GEOAPIFY_API_KEY is not configured' });
  res.setHeader('Cache-Control', 'private, max-age=300');
  res.status(200).json({ ok: true, key });
}
