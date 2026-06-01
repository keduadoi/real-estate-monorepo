'use client';

import { useTranslations } from 'next-intl';
import { mapProvider } from '@/lib/maps';

// This component pulls in the active map provider — and through it, Leaflet,
// which touches `window` on import. It MUST be loaded via next/dynamic with
// ssr:false; PropertyMap.tsx is responsible for that.

interface Props {
  latitude: number;
  longitude: number;
  address: string;
}

const MapView = mapProvider.MapView;

export default function PropertyMapClient({ latitude, longitude, address }: Props) {
  const t = useTranslations('maps');

  return (
    <>
      <MapView
        latitude={latitude}
        longitude={longitude}
        marker={{ title: address }}
        className="h-96 w-full rounded-lg overflow-hidden"
      />
      <div className="mt-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 text-sm">
        <span className="text-gray-500">
          {t('attribution', { provider: mapProvider.name })}
        </span>
        <a
          href={mapProvider.externalLink(latitude, longitude, address)}
          target="_blank"
          rel="noopener noreferrer"
          className="text-primary-600 hover:text-primary-700 font-medium inline-flex items-center"
        >
          {t('openExternal')}
          <svg
            className="w-4 h-4 ml-1"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
            />
          </svg>
        </a>
      </div>
    </>
  );
}
