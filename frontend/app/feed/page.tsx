import { getServerSession } from 'next-auth';
import { getTranslations } from 'next-intl/server';
import { authOptions } from '@/lib/auth';
import { postApi } from '@/lib/api/postApi';
import FeedPageClient from './FeedPageClient';

export async function generateMetadata() {
  const t = await getTranslations('metadata');
  return {
    title: t('feedTitle'),
    description: t('feedDescription'),
  };
}

export const dynamic = 'force-dynamic';

export default async function FeedPage() {
  const t = await getTranslations('feed');
  const session = await getServerSession(authOptions);
  const isAuthenticated = !!session?.user;
  const accessToken = session?.accessToken as string | undefined;

  let initialPosts: Awaited<ReturnType<typeof postApi.getFeed>>['data'] = [];
  let totalPosts = 0;
  let hasMore = false;

  try {
    const response = await postApi.getFeed(0, 20, { accessToken });
    initialPosts = response.data;
    totalPosts = response.total;
    hasMore = response.page < response.totalPages - 1;
  } catch (error) {
    console.error('Error fetching initial posts:', error);
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            {t('heading')}
          </h1>
          <p className="text-gray-600">
            {t('subheading')}
          </p>
        </div>

        <FeedPageClient
          key={Date.now()}
          initialPosts={initialPosts}
          initialTotal={totalPosts}
          initialHasMore={hasMore}
          isAuthenticated={isAuthenticated}
        />
      </div>
    </div>
  );
}
