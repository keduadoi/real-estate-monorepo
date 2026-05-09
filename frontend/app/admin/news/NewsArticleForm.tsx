'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import { newsApi } from '@/lib/api/newsApi';
import { NewsArticleResponse } from '@/types/api';

interface Props {
  initial?: NewsArticleResponse;
  currentUser: {
    id: string;
    email?: string | null;
    name?: string | null;
    roles: string[];
  };
  accessToken?: string;
}

const CATEGORY_OPTIONS = [
  'Thị trường',
  'Phân tích',
  'Chính sách',
  'Dự án',
  'Đầu tư',
  'Phong thủy',
];

function toLocalInput(value?: string | null): string {
  if (!value) return '';
  try {
    const d = new Date(value);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return '';
  }
}

export default function NewsArticleForm({ initial, currentUser, accessToken }: Props) {
  const router = useRouter();
  const isEdit = !!initial;

  const [title, setTitle] = useState(initial?.title ?? '');
  const [summary, setSummary] = useState(initial?.summary ?? '');
  const [content, setContent] = useState(initial?.content ?? '');
  const [author, setAuthor] = useState(initial?.author ?? '');
  const [category, setCategory] = useState(initial?.category ?? CATEGORY_OPTIONS[0]);
  const [imageUrl, setImageUrl] = useState(initial?.imageUrl ?? '');
  const [publishedAt, setPublishedAt] = useState(toLocalInput(initial?.publishedAt));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    const payload = {
      title: title.trim(),
      summary: summary.trim(),
      content: content.trim(),
      author: author.trim() || undefined,
      category: category.trim() || undefined,
      imageUrl: imageUrl.trim() || undefined,
      publishedAt: publishedAt ? new Date(publishedAt).toISOString() : undefined,
    };

    try {
      const opts = { user: currentUser, accessToken };
      const saved = isEdit
        ? await newsApi.update(initial!.id, payload, opts)
        : await newsApi.create(payload, opts);
      router.push(`/admin/news`);
      router.refresh();
      return saved;
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Lưu không thành công');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="bg-white shadow-sm border border-gray-200 rounded-lg p-6 space-y-4">
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-md p-3 text-sm">
          {error}
        </div>
      )}

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Tiêu đề *</label>
        <input
          type="text"
          required
          maxLength={300}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Tóm tắt *</label>
        <textarea
          required
          maxLength={500}
          rows={2}
          value={summary}
          onChange={(e) => setSummary(e.target.value)}
          className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        <div className="text-xs text-gray-500 mt-1">{summary.length}/500</div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Nội dung *</label>
        <textarea
          required
          rows={10}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Tác giả</label>
          <input
            type="text"
            maxLength={100}
            value={author}
            onChange={(e) => setAuthor(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Danh mục</label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            {CATEGORY_OPTIONS.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">URL ảnh đại diện</label>
        <input
          type="url"
          maxLength={1000}
          value={imageUrl}
          onChange={(e) => setImageUrl(e.target.value)}
          placeholder="https://images.unsplash.com/..."
          className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Thời điểm đăng</label>
        <input
          type="datetime-local"
          value={publishedAt}
          onChange={(e) => setPublishedAt(e.target.value)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        <p className="text-xs text-gray-500 mt-1">Bỏ trống để dùng thời điểm hiện tại (khi tạo mới).</p>
      </div>

      <div className="flex items-center gap-3 pt-2 border-t border-gray-100">
        <button
          type="submit"
          disabled={submitting}
          className="bg-primary-600 hover:bg-primary-700 text-white px-5 py-2 rounded-md text-sm font-medium disabled:opacity-50"
        >
          {submitting ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Tạo bài viết'}
        </button>
        <button
          type="button"
          onClick={() => router.push('/admin/news')}
          className="px-5 py-2 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Hủy
        </button>
      </div>
    </form>
  );
}
