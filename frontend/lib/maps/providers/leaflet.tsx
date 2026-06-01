'use client';

import { useEffect } from 'react';
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { MapProvider, MapViewProps } from '../types';

// Leaflet's default marker icon is loaded via webpack-relative URLs that break
// under Next.js. Point the default icon at the CDN copies so markers render.
// Side-effect runs once on module load.
const ICON_BASE = 'https://unpkg.com/leaflet@1.9.4/dist/images';
L.Icon.Default.mergeOptions({
  iconRetinaUrl: `${ICON_BASE}/marker-icon-2x.png`,
  iconUrl: `${ICON_BASE}/marker-icon.png`,
  shadowUrl: `${ICON_BASE}/marker-shadow.png`,
});

// OSM's public tile server is fine for development and light production.
// Once traffic exceeds ~10k tile loads/day, swap this URL for a free-tier
// provider like MapTiler or Stadia — no other change required.
const OSM_TILE_URL = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
const OSM_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

/**
 * Re-centers the map when the props change. MapContainer's `center` prop is
 * only read once on mount, so we need this companion to react to navigation
 * between different properties without remounting the whole map.
 */
function ReCenter({ latitude, longitude }: { latitude: number; longitude: number }) {
  const map = useMap();
  useEffect(() => {
    map.setView([latitude, longitude], map.getZoom(), { animate: false });
  }, [latitude, longitude, map]);
  return null;
}

function LeafletMapView({
  latitude,
  longitude,
  zoom = 15,
  marker,
  className,
}: MapViewProps) {
  return (
    <MapContainer
      center={[latitude, longitude]}
      zoom={zoom}
      scrollWheelZoom={false}
      className={className ?? 'h-96 w-full rounded-lg'}
    >
      <TileLayer attribution={OSM_ATTRIBUTION} url={OSM_TILE_URL} />
      <ReCenter latitude={latitude} longitude={longitude} />
      <Marker position={[latitude, longitude]}>
        {marker && (
          <Popup>
            <div className="font-semibold">{marker.title}</div>
            {marker.description && (
              <div className="text-xs text-gray-600 mt-1">{marker.description}</div>
            )}
          </Popup>
        )}
      </Marker>
    </MapContainer>
  );
}

export const LeafletProvider: MapProvider = {
  MapView: LeafletMapView,
  name: 'OpenStreetMap',
  externalLink: (lat, lng) =>
    `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=17/${lat}/${lng}`,
};
