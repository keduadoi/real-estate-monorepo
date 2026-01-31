import Link from 'next/link';
import Image from 'next/image';
import { Property } from '@/types';
import { formatPrice, formatArea, fixImageUrl } from '@/lib/utils';

interface PropertyCardProps {
  property: Property;
}

export default function PropertyCard({ property }: PropertyCardProps) {
  const statusText =
    property.status === 'for-sale' ? 'Cần bán' : 'Cho thuê';
  const statusColor =
    property.status === 'for-sale'
      ? 'bg-green-100 text-green-800'
      : 'bg-blue-100 text-blue-800';

  const typeText = {
    house: 'Nhà',
    apartment: 'Căn hộ',
    villa: 'Biệt thự',
    townhouse: 'Nhà phố',
  }[property.propertyType];

  return (
    <Link href={`/properties/${property.id}`}>
      <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-xl transition-shadow duration-300 cursor-pointer h-full flex flex-col">
        <div className="relative h-48 w-full">
          <Image
            src={fixImageUrl(property.images?.[0])}
            alt={property.title}
            fill
            className="object-cover"
            sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
          />
          <div className="absolute top-2 right-2">
            <span
              className={`px-2 py-1 rounded-full text-xs font-semibold ${statusColor}`}
            >
              {statusText}
            </span>
          </div>
        </div>

        <div className="p-4 flex-1 flex flex-col">
          <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
            {property.title}
          </h3>

          <p className="text-2xl font-bold text-primary-600 mb-2">
            {formatPrice(property.price)}
            {property.status === 'for-rent' && (
              <span className="text-sm font-normal text-gray-500">/tháng</span>
            )}
          </p>

          <p className="text-sm text-gray-600 mb-3 flex items-center">
            <svg
              className="w-4 h-4 mr-1"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
            {property.address}
          </p>

          <div className="flex items-center justify-between text-sm text-gray-500 mt-auto pt-3 border-t border-gray-100">
            <div className="flex items-center">
              <svg
                className="w-4 h-4 mr-1"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
                />
              </svg>
              <span>{property.bedrooms} PN</span>
            </div>
            <div className="flex items-center">
              <svg
                className="w-4 h-4 mr-1"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M8 14v3m4-3v3m4-3v3M3 21h18M3 10h18M3 7l9-4 9 4M4 10h16v11H4V10z"
                />
              </svg>
              <span>{property.bathrooms} WC</span>
            </div>
            <div className="flex items-center">
              <svg
                className="w-4 h-4 mr-1"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5v-4m0 4h-4m4 0l-5-5"
                />
              </svg>
              <span>{formatArea(property.area)}</span>
            </div>
          </div>

          <div className="mt-2">
            <span className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
              {typeText}
            </span>
          </div>
        </div>
      </div>
    </Link>
  );
}
