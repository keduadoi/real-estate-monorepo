import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { postApi } from '@/lib/api/postApi';
import FeedPageClient from './FeedPageClient';

export const metadata = {
  title: 'Bảng tin cộng đồng | Real Estate App',
  description: 'Chia sẻ và thảo luận về bất động sản cùng cộng đồng',
};

export const dynamic = 'force-dynamic';

export default async function FeedPage() {
  // Get session
  const session = await getServerSession(authOptions);
  const isAuthenticated = !!session?.user;
  const accessToken = session?.accessToken as string | undefined;

  // Fetch initial posts from the real API
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
    // Continue with empty posts - the client will show an error state
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Bảng tin cộng đồng
          </h1>
          <p className="text-gray-600">
            Chia sẻ kinh nghiệm, thông tin và thảo luận về thị trường bất động sản
          </p>
        </div>

        {/* Client-side components - key forces remount on server re-render */}
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
