export default function handler(req, res) {
  const token = process.env.MAPBOX_ACCESS_TOKEN || process.env.NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN || '';
  res.setHeader('Cache-Control', 'no-store, max-age=0');
  res.status(200).json({ token });
}
