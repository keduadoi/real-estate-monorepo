'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { newsApi } from '@/lib/api/newsApi';
import { NewsArticleSummary } from '@/types/api';

interface Props {
  articles: NewsArticleSummary[];
  currentUser: {
    id: string;
    email?: string | null;
    name?: string | null;
    roles: string[];
  };
  accessToken?: string;
  currentPage: number;
  totalPages: number;
}

export default function AdminNewsTable({
  articles,
  currentUser,
  accessToken,
  currentPage,
  totalPages,
}: Props) {
  const router = useRouter();
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  function formatDate(value: string | null) {
    if (!value) return '—';
    try {
      return new Date(value).toLocaleString('vi-VN');
    } catch {
      return value;
    }
  }

  async function handleDelete(id: number, title: string) {
    if (!confirm(`Xóa bài "${title}"?`)) return;
    setDeletingId(id);
    setError(null);
    try {
      await newsApi.remove(id, { user: currentUser, accessToken });
      router.refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Xóa không thành công');
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <>
      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 rounded-md p-3 text-sm">
          {error}
        </div>
      )}

      <div className="bg-white shadow-sm border border-gray-200 rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600">
            <tr>
              <th className="text-left px-4 py-3 font-medium">ID</th>
              <th className="text-left px-4 py-3 font-medium">Tiêu đề</th>
              <th className="text-left px-4 py-3 font-medium">Danh mục</th>
              <th className="text-left px-4 py-3 font-medium">Tác giả</th>
              <th className="text-left px-4 py-3 font-medium">Đăng lúc</th>
              <th className="text-right px-4 py-3 font-medium">Hành động</th>
            </tr>
          </thead>
          <tbody>
            {articles.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center py-8 text-gray-500">
                  Không có bài viết nào.
                </td>
              </tr>
            ) : (
              articles.map((a) => (
                <tr key={a.id} className="border-t border-gray-100 hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-700">{a.id}</td>
                  <td className="px-4 py-3 text-gray-900 max-w-md">
                    <div className="font-medium line-clamp-1">{a.title}</div>
                    <div className="text-xs text-gray-500 line-clamp-1">{a.summary}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-700">{a.category ?? '—'}</td>
                  <td className="px-4 py-3 text-gray-700">{a.author ?? '—'}</td>
                  <td className="px-4 py-3 text-gray-700">{formatDate(a.publishedAt)}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="inline-flex gap-2">
                      <Link
                        href={`/news/${a.id}`}
                        className="text-gray-600 hover:text-primary-600"
                      >
                        Xem
                      </Link>
                      <Link
                        href={`/admin/news/${a.id}/edit`}
                        className="text-primary-600 hover:underline"
                      >
                        Sửa
                      </Link>
                      <button
                        onClick={() => handleDelete(a.id, a.title)}
                        disabled={deletingId === a.id}
                        className="text-red-600 hover:underline disabled:opacity-50"
                      >
                        {deletingId === a.id ? 'Đang xóa…' : 'Xóa'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-6">
          {currentPage > 1 && (
            <Link
              href={`/admin/news?page=${currentPage - 1}`}
              className="px-3 py-2 rounded-md border border-gray-300 text-sm hover:bg-gray-50"
            >
              ← Trước
            </Link>
          )}
          <span className="px-3 py-2 text-sm text-gray-600">
            Trang {currentPage} / {totalPages}
          </span>
          {currentPage < totalPages && (
            <Link
              href={`/admin/news?page=${currentPage + 1}`}
              className="px-3 py-2 rounded-md border border-gray-300 text-sm hover:bg-gray-50"
            >
              Tiếp →
            </Link>
          )}
        </div>
      )}
    </>
  );
}
