import SearchBar from '@/components/SearchBar';
import PropertyGrid from '@/components/PropertyGrid';
import Pagination from '@/components/Pagination';
import { propertyApi } from '@/lib/api/propertyApi';
import { mapApiPropertyToUi } from '@/lib/api/mapper';
import { SortOption } from '@/lib/utils';

interface HomePageProps {
  searchParams: {
    page?: string;
    sort?: SortOption;
  };
}

export default async function HomePage({ searchParams }: HomePageProps) {
  const currentPage = Number(searchParams.page) || 1;
  const perPage = 12;

  // Map sort option to API parameters
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

  // Fetch properties from API (page is 0-indexed in backend)
  const apiResponse = await propertyApi.getAll(currentPage - 1, perPage, sortBy, sortDirection);

  // Convert API properties to UI properties
  const paginatedResult = {
    data: apiResponse.data.map(mapApiPropertyToUi),
    total: apiResponse.total,
    page: apiResponse.page + 1, // Convert back to 1-indexed for UI
    perPage: apiResponse.perPage,
    totalPages: apiResponse.totalPages,
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Hero Section */}
      <div className="text-center mb-12">
        <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-4">
          Tìm kiếm Bất động sản
        </h1>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Khám phá hàng trăm bất động sản từ nhà phố, căn hộ đến biệt thự tại
          Việt Nam. Tìm ngôi nhà mơ ước của bạn ngay hôm nay.
        </p>
      </div>

      {/* Search Bar */}
      <SearchBar />

      {/* Sort Indicator */}
      {searchParams.sort && (
        <div className="mb-4">
          <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
            Sắp xếp:{' '}
            {{
              newest: 'Mới nhất',
              oldest: 'Cũ nhất',
              'price-low': 'Giá thấp - cao',
              'price-high': 'Giá cao - thấp',
            }[searchParams.sort]}
          </span>
        </div>
      )}

      {/* Results Info */}
      <div className="mb-6">
        <h2 className="text-2xl font-semibold text-gray-900">
          Bất động sản mới nhất
        </h2>
        <p className="text-gray-600 mt-1">
          Hiển thị {paginatedResult.data.length} trong tổng số{' '}
          {paginatedResult.total} bất động sản
        </p>
      </div>

      {/* Property Grid */}
      <PropertyGrid properties={paginatedResult.data} />

      {/* Pagination */}
      <Pagination
        currentPage={paginatedResult.page}
        totalPages={paginatedResult.totalPages}
      />
    </div>
  );
}
