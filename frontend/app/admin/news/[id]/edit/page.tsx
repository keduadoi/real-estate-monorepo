import Link from 'next/link';
import { notFound } from 'next/navigation';
import { getTranslations } from 'next-intl/server';
import { newsApi } from '@/lib/api/newsApi';
import { requireAdmin } from '@/lib/auth-helpers/requireAdmin';
import NewsArticleForm from '../../NewsArticleForm';

export async function generateMetadata() {
  const t = await getTranslations('metadata');
  return { title: t('adminNewsEditTitle') };
}

export const dynamic = 'force-dynamic';

interface EditNewsPageProps {
  params: { id: string };
}

export default async function EditNewsPage({ params }: EditNewsPageProps) {
  const session = await requireAdmin();
  const t = await getTranslations('admin.news.edit');
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
          {t('back')}
        </Link>
        <h1 className="text-2xl font-bold text-gray-900 mt-2">
          {t('heading', { title: article.title })}
        </h1>
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
