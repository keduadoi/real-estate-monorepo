import Link from 'next/link';
import { newsApi } from '@/lib/api/newsApi';
import { requireAdmin } from '@/lib/auth-helpers/requireAdmin';
import AdminNewsTable from './AdminNewsTable';

export const metadata = {
  title: 'Quản trị tin tức | BĐS Vietnam',
};

export const dynamic = 'force-dynamic';

interface AdminNewsPageProps {
  searchParams: { page?: string };
}

const PER_PAGE = 20;

export default async function AdminNewsPage({ searchParams }: AdminNewsPageProps) {
  const session = await requireAdmin();
  const currentPage = Math.max(1, Number(searchParams.page) || 1);

  const response = await newsApi.list(currentPage - 1, PER_PAGE);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quản trị tin tức</h1>
          <p className="text-sm text-gray-600 mt-1">
            Tổng {response.total} bài viết — Trang {currentPage}/{response.totalPages || 1}
          </p>
        </div>
        <Link
          href="/admin/news/new"
          className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-md text-sm font-medium"
        >
          + Tạo bài mới
        </Link>
      </div>

      <AdminNewsTable
        articles={response.data}
        currentUser={{
          id: session.user.id,
          email: session.user.email,
          name: session.user.name,
          roles: session.user.roles ?? [],
        }}
        accessToken={(session as any).accessToken}
        totalPages={response.totalPages}
        currentPage={currentPage}
      />
    </div>
  );
}
