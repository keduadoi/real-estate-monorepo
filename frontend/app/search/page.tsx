import Link from 'next/link';
import SearchBar from '@/components/SearchBar';
import PropertyGrid from '@/components/PropertyGrid';
import Pagination from '@/components/Pagination';
import { getMockProperties } from '@/lib/mockData';
import { filterProperties, paginateArray, sortProperties, SortOption } from '@/lib/utils';
import { PropertyType, PropertyStatus, SearchFilters } from '@/types';

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

export default function SearchPage({ searchParams }: SearchPageProps) {
  const currentPage = Number(searchParams.page) || 1;
  const perPage = 12;

  // Build filters object
  const filters: SearchFilters = {
    query: searchParams.query,
    city: searchParams.city,
    propertyType: searchParams.propertyType,
    status: searchParams.status,
    minPrice: searchParams.minPrice ? Number(searchParams.minPrice) * 1000000 : undefined,
    maxPrice: searchParams.maxPrice ? Number(searchParams.maxPrice) * 1000000 : undefined,
    bedrooms: searchParams.bedrooms ? Number(searchParams.bedrooms) : undefined,
  };

  // Filter properties
  const filteredProperties = filterProperties(getMockProperties(), filters);

  // Sort properties
  const sortedProperties = sortProperties(filteredProperties, searchParams.sort);

  // Paginate results
  const paginatedResult = paginateArray(sortedProperties, {
    page: currentPage,
    perPage,
  });

  // Check if any filters or sort are active
  const hasFilters = Object.values(filters).some((value) => value !== undefined) || searchParams.sort;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Page Header */}
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
          Quay lại trang chủ
        </Link>

        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Kết quả tìm kiếm
        </h1>
        {searchParams.query && (
          <p className="text-gray-600">
            Tìm kiếm cho: &quot;{searchParams.query}&quot;
          </p>
        )}
      </div>

      {/* Search Bar */}
      <SearchBar />

      {/* Active Filters */}
      {hasFilters && (
        <div className="mb-6 flex flex-wrap gap-2">
          {searchParams.query && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              Từ khóa: {searchParams.query}
            </span>
          )}
          {searchParams.city && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              Thành phố: {searchParams.city}
            </span>
          )}
          {searchParams.propertyType && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              Loại:{' '}
              {{
                house: 'Nhà',
                apartment: 'Căn hộ',
                villa: 'Biệt thự',
                townhouse: 'Nhà phố',
              }[searchParams.propertyType]}
            </span>
          )}
          {searchParams.status && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              Trạng thái:{' '}
              {searchParams.status === 'for-sale' ? 'Cần bán' : 'Cho thuê'}
            </span>
          )}
          {(searchParams.minPrice || searchParams.maxPrice) && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              Giá: {searchParams.minPrice || '0'} -{' '}
              {searchParams.maxPrice || '∞'} triệu
            </span>
          )}
          {searchParams.bedrooms && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              {searchParams.bedrooms}+ phòng ngủ
            </span>
          )}
          {searchParams.sort && (
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
              Sắp xếp:{' '}
              {{
                newest: 'Mới nhất',
                oldest: 'Cũ nhất',
                'price-low': 'Giá thấp - cao',
                'price-high': 'Giá cao - thấp',
              }[searchParams.sort]}
            </span>
          )}
        </div>
      )}

      {/* Results Info */}
      <div className="mb-6">
        <p className="text-gray-900 font-medium">
          {paginatedResult.total > 0 ? (
            <>
              Hiển thị {(currentPage - 1) * perPage + 1} -{' '}
              {Math.min(currentPage * perPage, paginatedResult.total)} trong
              tổng số {paginatedResult.total} kết quả
            </>
          ) : (
            'Không tìm thấy kết quả'
          )}
        </p>
      </div>

      {/* Property Grid */}
      <PropertyGrid properties={paginatedResult.data} />

      {/* Pagination */}
      {paginatedResult.totalPages > 1 && (
        <Pagination
          currentPage={paginatedResult.page}
          totalPages={paginatedResult.totalPages}
        />
      )}

      {/* No Results Message */}
      {paginatedResult.total === 0 && hasFilters && (
        <div className="text-center py-8">
          <Link
            href="/"
            className="inline-flex items-center text-primary-600 hover:text-primary-700 font-medium"
          >
            Xóa bộ lọc và xem tất cả bất động sản
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
