export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store, max-age=0');

  const backendBase = String(
    process.env.WASLHA_BACKEND_URL || 'https://waslha-backend.vercel.app'
  ).replace(/\/$/, '');

  try {
    const response = await fetch(`${backendBase}/api/mapbox-token`, {
      headers: { accept: 'application/json' },
      cache: 'no-store',
    });

    const data = await response.json().catch(() => null);

    if (!response.ok || !data?.success || !data?.data?.token) {
      return res.status(response.ok ? 503 : response.status).json({
        success: false,
        error: {
          code: data?.error?.code || 'MAPBOX_NOT_CONFIGURED',
          message: data?.error?.message || 'Mapbox is not configured on the backend',
        },
      });
    }

    return res.status(200).json(data);
  } catch (error) {
    return res.status(503).json({
      success: false,
      error: {
        code: 'MAPBOX_BACKEND_UNAVAILABLE',
        message: 'Unable to reach the Waslha backend for Mapbox configuration',
      },
    });
  }
}
