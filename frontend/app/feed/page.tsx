import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { getPostsWithMetadata } from '@/lib/mockData';
import FeedPageClient from './FeedPageClient';

export const metadata = {
  title: 'Bảng tin cộng đồng | Real Estate App',
  description: 'Chia sẻ và thảo luận về bất động sản cùng cộng đồng',
};

export default async function FeedPage() {
  // Get session
  const session = await getServerSession(authOptions);
  const isAuthenticated = !!session?.user;
  const currentUserId = session?.user?.id;

  // Fetch initial posts
  const posts = getPostsWithMetadata(currentUserId);

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

        {/* Client-side components */}
        <FeedPageClient
          initialPosts={posts}
          isAuthenticated={isAuthenticated}
        />
      </div>
    </div>
  );
}
