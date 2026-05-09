import Image from 'next/image';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { newsApi } from '@/lib/api/newsApi';

export const dynamic = 'force-dynamic';

interface NewsDetailProps {
  params: { id: string };
}

function formatDate(value: string | null): string {
  if (!value) return '';
  try {
    return new Date(value).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return '';
  }
}

export default async function NewsDetailPage({ params }: NewsDetailProps) {
  const id = Number(params.id);
  if (!Number.isFinite(id) || id <= 0) {
    notFound();
  }

  let article;
  try {
    article = await newsApi.getById(id);
  } catch (err) {
    console.error('Failed to load news article:', err);
    notFound();
  }

  return (
    <article className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6">
        <Link
          href="/news"
          className="text-sm text-primary-600 hover:underline inline-flex items-center"
        >
          ← Quay lại danh sách tin tức
        </Link>
      </div>

      {article.category && (
        <span className="inline-block bg-primary-100 text-primary-700 text-xs font-medium px-2.5 py-1 rounded mb-3">
          {article.category}
        </span>
      )}

      <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
        {article.title}
      </h1>

      <div className="flex items-center gap-4 text-sm text-gray-500 mb-6">
        <span>Tác giả: {article.author ?? 'BĐS Vietnam'}</span>
        <span>•</span>
        <span>{formatDate(article.publishedAt)}</span>
      </div>

      {article.imageUrl && (
        <div className="relative w-full h-64 sm:h-80 md:h-96 mb-6 bg-gray-100 rounded-lg overflow-hidden">
          <Image
            src={article.imageUrl}
            alt={article.title}
            fill
            sizes="(max-width: 768px) 100vw, 768px"
            className="object-cover"
            unoptimized
            priority
          />
        </div>
      )}

      <p className="text-lg text-gray-700 font-medium mb-6 leading-relaxed">
        {article.summary}
      </p>

      <div className="prose prose-gray max-w-none whitespace-pre-line text-gray-800 leading-relaxed">
        {article.content}
      </div>
    </article>
  );
}
