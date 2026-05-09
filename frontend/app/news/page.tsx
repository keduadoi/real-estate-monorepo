import Link from 'next/link';
import Image from 'next/image';
import { getLocale, getTranslations } from 'next-intl/server';
import { newsApi } from '@/lib/api/newsApi';
import Pagination from '@/components/Pagination';
import { NewsArticleSummary } from '@/types/api';
import { toIntlLocale } from '@/lib/i18n/intlLocale';

export async function generateMetadata() {
  const t = await getTranslations('metadata');
  return {
    title: t('newsTitle'),
    description: t('newsDescription'),
  };
}

export const dynamic = 'force-dynamic';

interface NewsPageProps {
  searchParams: {
    page?: string;
    category?: string;
  };
}

const PER_PAGE = 12;

function formatDate(value: string | null, locale: string): string {
  if (!value) return '';
  try {
    return new Date(value).toLocaleDateString(locale, {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return '';
  }
}

const CATEGORY_KEYS = ['Thị trường', 'Phân tích', 'Chính sách', 'Dự án', 'Đầu tư', 'Phong thủy'];

export default async function NewsPage({ searchParams }: NewsPageProps) {
  const t = await getTranslations();
  const locale = toIntlLocale(await getLocale());
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
    errorMessage = t('news.errorLoading');
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-2">
          {t('news.heading')}
        </h1>
        <p className="text-gray-600">
          {t('news.subheading')}
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
          {t('common.all')}
        </Link>
        {CATEGORY_KEYS.map((cat) => (
          <Link
            key={cat}
            href={`/news?category=${encodeURIComponent(cat)}`}
            className={`px-3 py-1.5 rounded-full text-sm font-medium border ${
              category === cat
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-primary-600'
            }`}
          >
            {t(`common.newsCategories.${cat}` as any)}
          </Link>
        ))}
      </div>

      {errorMessage ? (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-md p-4">
          {errorMessage}
        </div>
      ) : articles.length === 0 ? (
        <div className="text-center py-16 text-gray-500">{t('news.empty')}</div>
      ) : (
        <>
          <div className="mb-4 text-sm text-gray-600">
            {t('news.total', { count: total })}
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
                      {t('news.noImage')}
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
                    <span>{article.author ?? t('news.defaultAuthor')}</span>
                    <span>{formatDate(article.publishedAt, locale)}</span>
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
