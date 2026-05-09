import Link from 'next/link';
import { getTranslations } from 'next-intl/server';
import SearchBar from '@/components/SearchBar';
import PropertyGrid from '@/components/PropertyGrid';
import Pagination from '@/components/Pagination';
import { propertyApi } from '@/lib/api/propertyApi';
import { mapApiPropertyToUi, mapUiPropertyTypeToApi, mapUiPropertyStatusToApi } from '@/lib/api/mapper';
import { SortOption } from '@/lib/utils';
import { PropertyType, PropertyStatus } from '@/types';
import { PropertySearchRequest } from '@/types/api';

interface SearchPageProps {
  searchParams: {
    query?: string;
    city?: string;
    propertyType?: PropertyType;
    status?: PropertyStatus;
    minPrice?: string;
    maxPrice?: string;
    bedrooms?: string;
    page?: string;
    sort?: SortOption;
  };
}

const SORT_KEYS: Record<SortOption, 'newest' | 'oldest' | 'priceLow' | 'priceHigh'> = {
  newest: 'newest',
  oldest: 'oldest',
  'price-low': 'priceLow',
  'price-high': 'priceHigh',
};

export default async function SearchPage({ searchParams }: SearchPageProps) {
  const t = await getTranslations();
  const currentPage = Number(searchParams.page) || 1;
  const perPage = 12;

  const getSortParams = (sort?: SortOption): { sortBy: string; sortDirection: 'asc' | 'desc' } => {
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
  };

  const { sortBy, sortDirection } = getSortParams(searchParams.sort);

  const apiSearchRequest: PropertySearchRequest = {
    query: searchParams.query,
    city: searchParams.city,
    propertyType: searchParams.propertyType ? mapUiPropertyTypeToApi(searchParams.propertyType) : undefined,
    status: searchParams.status ? mapUiPropertyStatusToApi(searchParams.status) : undefined,
    minPrice: searchParams.minPrice ? Number(searchParams.minPrice) * 1000000 : undefined,
    maxPrice: searchParams.maxPrice ? Number(searchParams.maxPrice) * 1000000 : undefined,
    bedrooms: searchParams.bedrooms ? Number(searchParams.bedrooms) : undefined,
    sortBy,
    sortDirection,
  };

  const apiResponse = await propertyApi.search(apiSearchRequest, currentPage - 1, perPage);

  const paginatedResult = {
    data: apiResponse.data.map(mapApiPropertyToUi),
    total: apiResponse.total,
    page: apiResponse.page + 1,
    perPage: apiResponse.perPage,
    totalPages: apiResponse.totalPages,
  };

  const hasFilters = Object.values(apiSearchRequest).some((value) => value !== undefined && value !== sortBy && value !== sortDirection);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <Link
          href="/"
          className="text-primary-600 hover:text-primary-700 text-sm font-medium inline-flex items-center mb-4"
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
          {t('search.backToHome')}
        </Link>

        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          {t('search.heading')}
        </h1>
        {searchParams.query && (
          <p className="text-gray-600">
            {t('search.searchingFor', { query: searchParams.query })}
          </p>
        )}
      </div>

      <SearchBar />

      {hasFilters && (
        <div className="mb-6 flex flex-wrap gap-2">
          {searchParams.query && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              {t('search.filters.keyword', { value: searchParams.query })}
            </span>
          )}
          {searchParams.city && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              {t('search.filters.city', { value: searchParams.city })}
            </span>
          )}
          {searchParams.propertyType && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              {t('search.filters.type', { value: t(`common.propertyType.${searchParams.propertyType}`) })}
            </span>
          )}
          {searchParams.status && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              {t('search.filters.status', { value: t(`common.propertyStatus.${searchParams.status === 'for-sale' ? 'forSale' : 'forRent'}`) })}
            </span>
          )}
          {(searchParams.minPrice || searchParams.maxPrice) && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              {t('search.filters.price', {
                min: searchParams.minPrice || '0',
                max: searchParams.maxPrice || t('search.filters.priceUnlimited'),
              })}
            </span>
          )}
          {searchParams.bedrooms && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              {t('search.filters.bedrooms', { count: searchParams.bedrooms })}
            </span>
          )}
          {searchParams.sort && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
              {t('search.filters.sortPrefix')}{' '}
              {t(`common.sort.${SORT_KEYS[searchParams.sort]}`)}
            </span>
          )}
        </div>
      )}

      <div className="mb-6">
        <p className="text-gray-900 font-medium">
          {paginatedResult.total > 0
            ? t('search.showing', {
                from: (currentPage - 1) * perPage + 1,
                to: Math.min(currentPage * perPage, paginatedResult.total),
                total: paginatedResult.total,
              })
            : t('search.noResults')}
        </p>
      </div>

      <PropertyGrid properties={paginatedResult.data} />

      {paginatedResult.totalPages > 1 && (
        <Pagination
          currentPage={paginatedResult.page}
          totalPages={paginatedResult.totalPages}
        />
      )}

      {paginatedResult.total === 0 && hasFilters && (
        <div className="text-center py-8">
          <Link
            href="/"
            className="inline-flex items-center text-primary-600 hover:text-primary-700 font-medium"
          >
            {t('search.clearFilters')}
            <svg
              className="w-5 h-5 ml-1"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M13 7l5 5m0 0l-5 5m5-5H6"
              />
            </svg>
          </Link>
        </div>
      )}
    </div>
  );
}
