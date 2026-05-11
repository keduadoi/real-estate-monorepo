import { getTranslations } from 'next-intl/server';
import { requireAdmin } from '@/lib/auth-helpers/requireAdmin';
import AdminCommentsClient from './AdminCommentsClient';

export async function generateMetadata() {
  return { title: 'Bình luận đang chờ xử lý' };
}

export const dynamic = 'force-dynamic';

export default async function AdminCommentsPage() {
  const session = await requireAdmin();
  const t = await getTranslations();

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">
        {t('admin.comments.heading')}
      </h1>
      <p className="text-sm text-gray-600 mb-6">
        {t('admin.comments.description')}
      </p>

      <AdminCommentsClient
        userId={session.user.id}
        userEmail={session.user.email}
        userName={session.user.name}
        userRoles={session.user.roles ?? []}
        accessToken={(session as { accessToken?: string }).accessToken}
      />
    </div>
  );
}
