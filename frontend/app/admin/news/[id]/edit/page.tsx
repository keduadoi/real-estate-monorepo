import Link from 'next/link';
import { notFound } from 'next/navigation';
import { newsApi } from '@/lib/api/newsApi';
import { requireAdmin } from '@/lib/auth-helpers/requireAdmin';
import NewsArticleForm from '../../NewsArticleForm';

export const metadata = {
  title: 'Chỉnh sửa bài viết | Quản trị tin tức',
};

export const dynamic = 'force-dynamic';

interface EditNewsPageProps {
  params: { id: string };
}

export default async function EditNewsPage({ params }: EditNewsPageProps) {
  const session = await requireAdmin();
  const id = Number(params.id);
  if (!Number.isFinite(id) || id <= 0) {
    notFound();
  }

  let article;
  try {
    article = await newsApi.getById(id);
  } catch {
    notFound();
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6">
        <Link href="/admin/news" className="text-sm text-primary-600 hover:underline">
          ← Quay lại danh sách
        </Link>
        <h1 className="text-2xl font-bold text-gray-900 mt-2">Chỉnh sửa: {article.title}</h1>
      </div>

      <NewsArticleForm
        initial={article}
        currentUser={{
          id: session.user.id,
          email: session.user.email,
          name: session.user.name,
          roles: session.user.roles ?? [],
        }}
        accessToken={(session as any).accessToken}
      />
    </div>
  );
}
