import { notFound } from 'next/navigation';
import Link from 'next/link';
import { getLocale, getTranslations } from 'next-intl/server';
import ImageGallery from '@/components/ImageGallery';
import PropertyActionButtons from '@/components/PropertyActionButtons';
import PriceHistory from '@/components/PriceHistory';
import PropertyComments from '@/components/PropertyComments';
import { propertyApi } from '@/lib/api/propertyApi';
import { mapApiPropertyToUi } from '@/lib/api/mapper';
import { formatPrice, formatArea } from '@/lib/utils';
import { toIntlLocale } from '@/lib/i18n/intlLocale';

interface PropertyDetailPageProps {
  params: {
    id: string;
  };
}

export default async function PropertyDetailPage({ params }: PropertyDetailPageProps) {
  const t = await getTranslations();
  const intlLocale = toIntlLocale(await getLocale());

  let property;

  try {
    const apiProperty = await propertyApi.getById(Number(params.id));
    property = mapApiPropertyToUi(apiProperty);
  } catch (error) {
    notFound();
  }

  const statusKey = property.status === 'for-sale' ? 'forSale' : 'forRent';
  const statusText = t(`common.propertyStatus.${statusKey}`);
  const statusColor =
    property.status === 'for-sale'
      ? 'bg-green-100 text-green-800'
      : 'bg-blue-100 text-blue-800';

  const typeText = t(`common.propertyType.${property.propertyType}`);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <Link
          href="/"
          className="text-primary-600 hover:text-primary-700 text-sm font-medium inline-flex items-center"
        >
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
              d="M15 19l-7-7 7-7"
            />
          </svg>
          {t('properties.detail.back')}
        </Link>

        <PropertyActionButtons
          propertyId={property.id}
          propertyUserId={property.userId}
          propertyTitle={property.title}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <ImageGallery images={property.images} title={property.title} />

          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-start justify-between mb-4">
              <div>
                <h1 className="text-3xl font-bold text-gray-900 mb-2">
                  {property.title}
                </h1>
                <p className="text-gray-600 flex items-center">
                  <svg
                    className="w-5 h-5 mr-1"
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
              </div>
              <span
                className={`px-3 py-1 rounded-full text-sm font-semibold ${statusColor}`}
              >
                {statusText}
              </span>
            </div>

            <div className="mb-6 pb-6 border-b">
              <p className="text-4xl font-bold text-primary-600">
                {formatPrice(property.price)}
                {property.status === 'for-rent' && (
                  <span className="text-lg font-normal text-gray-500">
                    {t('common.perMonth')}
                  </span>
                )}
              </p>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6 pb-6 border-b">
              <div className="text-center p-4 bg-gray-50 rounded-lg">
                <svg
                  className="w-8 h-8 mx-auto mb-2 text-primary-600"
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
                <p className="text-2xl font-bold text-gray-900">
                  {property.bedrooms}
                </p>
                <p className="text-sm text-gray-600">{t('properties.detail.stats.bedrooms')}</p>
              </div>

              <div className="text-center p-4 bg-gray-50 rounded-lg">
                <svg
                  className="w-8 h-8 mx-auto mb-2 text-primary-600"
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
                <p className="text-2xl font-bold text-gray-900">
                  {property.bathrooms}
                </p>
                <p className="text-sm text-gray-600">{t('properties.detail.stats.bathrooms')}</p>
              </div>

              <div className="text-center p-4 bg-gray-50 rounded-lg">
                <svg
                  className="w-8 h-8 mx-auto mb-2 text-primary-600"
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
                <p className="text-2xl font-bold text-gray-900">
                  {property.area}
                </p>
                <p className="text-sm text-gray-600">m²</p>
              </div>

              <div className="text-center p-4 bg-gray-50 rounded-lg">
                <svg
                  className="w-8 h-8 mx-auto mb-2 text-primary-600"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
                  />
                </svg>
                <p className="text-lg font-bold text-gray-900">{typeText}</p>
                <p className="text-sm text-gray-600">{t('properties.detail.stats.type')}</p>
              </div>
            </div>

            <div className="mb-6 pb-6 border-b">
              <h2 className="text-xl font-semibold text-gray-900 mb-3">
                {t('properties.detail.description')}
              </h2>
              <p className="text-gray-700 leading-relaxed whitespace-pre-line">
                {property.description}
              </p>
            </div>

            {property.features.length > 0 && (
              <div>
                <h2 className="text-xl font-semibold text-gray-900 mb-3">
                  {t('properties.detail.features')}
                </h2>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {property.features.map((feature) => {
                    const featureKey = `properties.form.featuresList.${feature}` as any;
                    const label = t.has(featureKey) ? t(featureKey) : feature;
                    return (
                      <div
                        key={feature}
                        className="flex items-center text-gray-700"
                      >
                        <svg
                          className="w-5 h-5 mr-2 text-green-500"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M5 13l4 4L19 7"
                          />
                        </svg>
                        {label}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          <PriceHistory propertyId={Number(property.id)} />
        </div>

        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-6 sticky top-4">
            <h3 className="text-xl font-semibold text-gray-900 mb-4">
              {t('properties.detail.contact.heading')}
            </h3>

            <div className="space-y-4">
              <button className="w-full bg-primary-600 text-white py-3 px-4 rounded-lg hover:bg-primary-700 transition-colors font-medium">
                {t('properties.detail.contact.call')}
              </button>
              <button className="w-full bg-white border-2 border-primary-600 text-primary-600 py-3 px-4 rounded-lg hover:bg-primary-50 transition-colors font-medium">
                {t('properties.detail.contact.message')}
              </button>
            </div>

            <div className="mt-6 pt-6 border-t">
              <h4 className="font-semibold text-gray-900 mb-3">
                {t('properties.detail.contact.infoHeading')}
              </h4>
              <div className="space-y-2 text-sm text-gray-600">
                <p>
                  <span className="font-medium">{t('properties.detail.contact.id')}</span> {property.id}
                </p>
                <p>
                  <span className="font-medium">{t('properties.detail.contact.city')}</span>{' '}
                  {property.city}
                </p>
                <p>
                  <span className="font-medium">{t('properties.detail.contact.postedAt')}</span>{' '}
                  {new Date(property.createdAt).toLocaleDateString(intlLocale)}
                </p>
              </div>
            </div>

            <div className="mt-6 pt-6 border-t">
              <p className="text-xs text-gray-500 text-center">
                {t('properties.detail.contact.footer')}
              </p>
            </div>
          </div>
        </div>
      </div>

      <PropertyComments propertyId={Number(property.id)} />
    </div>
  );
}
