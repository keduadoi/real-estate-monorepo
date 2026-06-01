import { LeafletProvider } from './providers/leaflet';
import { GoogleMapsProvider } from './providers/google';
import type { MapProvider } from './types';

export type { MapProvider, MapViewProps, MapMarker } from './types';

const providerName = process.env.NEXT_PUBLIC_MAP_PROVIDER ?? 'leaflet';

/**
 * The active map provider for this build.
 * Switch by setting NEXT_PUBLIC_MAP_PROVIDER at build time.
 *
 * Both providers are statically imported so the bundler can tree-shake the
 * unused one's runtime dependency (leaflet vs. @react-google-maps/api).
 * Once Google is wired, consider next/dynamic to code-split it instead.
 */
export const mapProvider: MapProvider =
  providerName === 'google' ? GoogleMapsProvider : LeafletProvider;
