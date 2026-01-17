import SearchBar from '@/components/SearchBar';
import PropertyGrid from '@/components/PropertyGrid';
import Pagination from '@/components/Pagination';
import { getMockProperties } from '@/lib/mockData';
import { paginateArray, sortProperties, SortOption } from '@/lib/utils';

interface HomePageProps {
  searchParams: {
    page?: string;
    sort?: SortOption;
  };
}

export default function HomePage({ searchParams }: HomePageProps) {
  const currentPage = Number(searchParams.page) || 1;
  const perPage = 12;

  // Get and sort properties
  const properties = getMockProperties();
  const sortedProperties = sortProperties(properties, searchParams.sort);

  const paginatedResult = paginateArray(sortedProperties, {
    page: currentPage,
    perPage,
  });

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
