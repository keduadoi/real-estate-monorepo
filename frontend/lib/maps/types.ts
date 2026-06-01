import type { ComponentType } from 'react';

export interface MapMarker {
  title: string;
  description?: string;
}

export interface MapViewProps {
  latitude: number;
  longitude: number;
  zoom?: number;
  marker?: MapMarker;
  className?: string;
}

/**
 * A swappable map renderer. The active provider is chosen by
 * NEXT_PUBLIC_MAP_PROVIDER at build time — feature code should only ever
 * import {@link mapProvider} from './index', never a concrete provider.
 */
export interface MapProvider {
  /** React component that renders the map. Must be safe to dynamic-import with ssr:false. */
  MapView: ComponentType<MapViewProps>;
  /** Display name used in attribution UI. */
  name: string;
  /** Build a deep link for "open in external maps app". */
  externalLink: (lat: number, lng: number, label?: string) => string;
}
