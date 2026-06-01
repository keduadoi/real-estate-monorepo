'use client';

import dynamic from 'next/dynamic';
import { useTranslations } from 'next-intl';
import type { GeocodingStatus } from '@/types';

// Load the client subcomponent — which is the only file that imports
// `lib/maps` and (transitively) Leaflet — lazily and never during SSR.
// Leaflet touches `window` on module load; loading it on the server crashes.
const PropertyMapClient = dynamic(() => import('./PropertyMapClient'), {
  ssr: false,
  loading: () => <MapSkeleton />,
});

function MapSkeleton() {
  return (
    <div
      className="h-96 w-full rounded-lg border border-gray-200 bg-gray-100 animate-pulse"
      aria-hidden
    />
  );
}

interface Props {
  latitude: number | null;
  longitude: number | null;
  address: string;
  geocodingStatus: GeocodingStatus;
}

export default function PropertyMap({
  latitude,
  longitude,
  address,
  geocodingStatus,
}: Props) {
  const t = useTranslations('maps');
  const hasCoords = latitude !== null && longitude !== null;

  return (
    <section className="bg-white rounded-lg shadow-md p-6 mt-8">
      <h2 className="text-xl font-semibold text-gray-900 mb-4">{t('heading')}</h2>

      {!hasCoords ? (
        <div className="h-32 w-full rounded-lg border border-dashed border-gray-300 bg-gray-50 flex items-center justify-center text-sm text-gray-500">
          {geocodingStatus === 'PENDING' ? t('pending') : t('unavailable')}
        </div>
      ) : (
        <PropertyMapClient
          latitude={latitude!}
          longitude={longitude!}
          address={address}
        />
      )}
    </section>
  );
}
