'use client';

import type { MapProvider } from '../types';

/**
 * Stub for the future Google Maps provider.
 *
 * To activate:
 *   1. `npm install @react-google-maps/api`
 *   2. Replace this file's MapView with a real implementation backed by
 *      <GoogleMap> from @react-google-maps/api.
 *   3. Set NEXT_PUBLIC_MAP_PROVIDER=google and NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=...
 *
 * No other file in the app needs to change — PropertyMap and the rest of the
 * feature code depend only on the MapProvider interface.
 */
function GoogleMapsStub(): JSX.Element {
  throw new Error(
    'GoogleMapsProvider is not implemented. Set NEXT_PUBLIC_MAP_PROVIDER=leaflet or ' +
      'implement the provider in lib/maps/providers/google.tsx.',
  );
}

export const GoogleMapsProvider: MapProvider = {
  MapView: GoogleMapsStub,
  name: 'Google Maps',
  externalLink: (lat, lng, label) =>
    `https://www.google.com/maps/search/?api=1&query=${lat},${lng}` +
    (label ? `&query_place_id=${encodeURIComponent(label)}` : ''),
};
