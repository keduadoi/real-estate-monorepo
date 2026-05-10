import { getTranslations } from 'next-intl/server';
import SearchModeSwitcher from '@/components/SearchModeSwitcher';
import CommonFiltersSidebar from '@/components/CommonFiltersSidebar';
import PropertyGrid from '@/components/PropertyGrid';
import Pagination from '@/components/Pagination';
import { propertyApi } from '@/lib/api/propertyApi';
import { mapApiPropertyToUi } from '@/lib/api/mapper';
import { SortOption } from '@/lib/utils';

export async function generateMetadata() {
  const t = await getTranslations('metadata');
  return {
    title: t('rentTitle'),
    description: t('rentDescription'),
  };
}

export const dynamic = 'force-dynamic';

interface RentPageProps {
  searchParams: {
    page?: string;
    sort?: SortOption;
  };
}

function getSortParams(sort?: SortOption): { sortBy: string; sortDirection: 'asc' | 'desc' } {
  switch (sort) {
    case 'newest':
      return { sortBy: 'createdAt', sortDirection: 'desc' };
    case 'oldest':
      return { sortBy: 'createdAt', sortDirection: 'asc' };
    case 'price-low':
      return { sortBy: 'price', sortDirection: 'asc' };
    case 'price-high':
      return { sortBy: 'price', sortDirection: 'desc' };
    default:
      return { sortBy: 'createdAt', sortDirection: 'desc' };
  }
}

const SORT_KEYS: Record<SortOption, 'newest' | 'oldest' | 'priceLow' | 'priceHigh'> = {
  newest: 'newest',
  oldest: 'oldest',
  'price-low': 'priceLow',
  'price-high': 'priceHigh',
};

export default async function RentPage({ searchParams }: RentPageProps) {
  const t = await getTranslations();
  const currentPage = Math.max(1, Number(searchParams.page) || 1);
  const perPage = 12;
  const { sortBy, sortDirection } = getSortParams(searchParams.sort);

  const apiResponse = await propertyApi.search(
    { status: 'FOR_RENT', sortBy, sortDirection },
    currentPage - 1,
    perPage,
  );

  const paginated = {
    data: apiResponse.data.map(mapApiPropertyToUi),
    total: apiResponse.total,
    page: apiResponse.page + 1,
    perPage: apiResponse.perPage,
    totalPages: apiResponse.totalPages,
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="text-center mb-12">
        <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-4">
          {t('rent.heading')}
        </h1>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          {t('rent.subheading')}
        </p>
      </div>

      <SearchModeSwitcher />

      <div className="lg:grid lg:grid-cols-[minmax(0,1fr)_280px] lg:gap-8">
        <div>
          {searchParams.sort && (
            <div className="mb-4">
              <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
                {t('rent.sortPrefix')}{' '}
                {t(`common.sort.${SORT_KEYS[searchParams.sort]}`)}
              </span>
            </div>
          )}

          <div className="mb-6">
            <h2 className="text-2xl font-semibold text-gray-900">{t('rent.sectionHeading')}</h2>
            <p className="text-gray-600 mt-1">
              {t('rent.showing', { shown: paginated.data.length, total: paginated.total })}
            </p>
          </div>

          <PropertyGrid properties={paginated.data} />

          <Pagination currentPage={paginated.page} totalPages={paginated.totalPages} />
        </div>

        <div className="hidden lg:block">
          <CommonFiltersSidebar status="for-rent" />
        </div>
      </div>
    </div>
  );
}
