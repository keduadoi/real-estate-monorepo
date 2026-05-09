import Link from 'next/link';
import { getTranslations } from 'next-intl/server';
import { requireAdmin } from '@/lib/auth-helpers/requireAdmin';
import NewsArticleForm from '../NewsArticleForm';

export async function generateMetadata() {
  const t = await getTranslations('metadata');
  return { title: t('adminNewsCreateTitle') };
}

export const dynamic = 'force-dynamic';

export default async function CreateNewsPage() {
  const session = await requireAdmin();
  const t = await getTranslations('admin.news.create');

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6">
        <Link href="/admin/news" className="text-sm text-primary-600 hover:underline">
          {t('back')}
        </Link>
        <h1 className="text-2xl font-bold text-gray-900 mt-2">{t('heading')}</h1>
      </div>

      <NewsArticleForm
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
