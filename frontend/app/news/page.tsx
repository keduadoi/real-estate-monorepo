import Link from 'next/link';
import Image from 'next/image';
import { newsApi } from '@/lib/api/newsApi';
import Pagination from '@/components/Pagination';
import { NewsArticleSummary } from '@/types/api';

export const metadata = {
  title: 'Tin tức bất động sản | BĐS Vietnam',
  description: 'Cập nhật tin tức, phân tích và xu hướng thị trường bất động sản mới nhất',
};

export const dynamic = 'force-dynamic';

interface NewsPageProps {
  searchParams: {
    page?: string;
    category?: string;
  };
}

const PER_PAGE = 12;

function formatDate(value: string | null): string {
  if (!value) return '';
  try {
    return new Date(value).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return '';
  }
}

export default async function NewsPage({ searchParams }: NewsPageProps) {
  const currentPage = Math.max(1, Number(searchParams.page) || 1);
  const category = searchParams.category;

  let articles: NewsArticleSummary[] = [];
  let totalPages = 0;
  let total = 0;
  let errorMessage: string | null = null;

  try {
    const response = await newsApi.list(currentPage - 1, PER_PAGE, category);
    articles = response.data;
    totalPages = response.totalPages;
    total = response.total;
  } catch (err) {
    console.error('Failed to load news:', err);
    errorMessage = 'Không thể tải tin tức ngay lúc này. Vui lòng thử lại sau.';
  }

  const categories = ['Thị trường', 'Phân tích', 'Chính sách', 'Dự án', 'Đầu tư', 'Phong thủy'];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-2">
          Tin tức bất động sản
        </h1>
        <p className="text-gray-600">
          Phân tích thị trường, chính sách mới và các dự án nổi bật.
        </p>
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        <Link
          href="/news"
          className={`px-3 py-1.5 rounded-full text-sm font-medium border ${
            !category
              ? 'bg-primary-600 text-white border-primary-600'
              : 'bg-white text-gray-700 border-gray-300 hover:border-primary-600'
          }`}
        >
          Tất cả
        </Link>
        {categories.map((cat) => (
          <Link
            key={cat}
            href={`/news?category=${encodeURIComponent(cat)}`}
            className={`px-3 py-1.5 rounded-full text-sm font-medium border ${
              category === cat
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-primary-600'
            }`}
          >
            {cat}
          </Link>
        ))}
      </div>

      {errorMessage ? (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-md p-4">
          {errorMessage}
        </div>
      ) : articles.length === 0 ? (
        <div className="text-center py-16 text-gray-500">Chưa có bài viết nào.</div>
      ) : (
        <>
          <div className="mb-4 text-sm text-gray-600">
            Tổng {total} bài viết
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {articles.map((article) => (
              <Link
                key={article.id}
                href={`/news/${article.id}`}
                className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow overflow-hidden border border-gray-100 flex flex-col"
              >
                <div className="relative w-full h-48 bg-gray-100">
                  {article.imageUrl ? (
                    <Image
                      src={article.imageUrl}
                      alt={article.title}
                      fill
                      sizes="(max-width: 768px) 100vw, (max-width: 1024px) 50vw, 33vw"
                      className="object-cover"
                      unoptimized
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-400">
                      No image
                    </div>
                  )}
                  {article.category && (
                    <span className="absolute top-3 left-3 bg-primary-600 text-white text-xs font-medium px-2 py-1 rounded">
                      {article.category}
                    </span>
                  )}
                </div>
                <div className="p-4 flex-1 flex flex-col">
                  <h2 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
                    {article.title}
                  </h2>
                  <p className="text-sm text-gray-600 mb-3 line-clamp-3 flex-1">
                    {article.summary}
                  </p>
                  <div className="flex items-center justify-between text-xs text-gray-500 mt-auto">
                    <span>{article.author ?? 'BĐS Vietnam'}</span>
                    <span>{formatDate(article.publishedAt)}</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>

          <Pagination currentPage={currentPage} totalPages={totalPages} />
        </>
      )}
    </div>
  );
}
