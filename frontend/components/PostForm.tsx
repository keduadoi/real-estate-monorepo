'use client';

import { useState } from 'react';
import { useSession } from 'next-auth/react';
import { postApi } from '@/lib/api/postApi';

interface PostFormProps {
  onPostCreated: () => void;
}

const MAX_CHARACTERS = 5000;

export default function PostForm({ onPostCreated }: PostFormProps) {
  const { data: session } = useSession();
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const characterCount = content.length;
  const isOverLimit = characterCount > MAX_CHARACTERS;
  const isValid = content.trim().length > 0 && !isOverLimit;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isValid) return;

    const accessToken = session?.accessToken as string | undefined;
    if (!accessToken) {
      setError('Vui lòng đăng nhập để đăng bài viết');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      await postApi.create(
        { content: content.trim() },
        { accessToken }
      );

      // Clear form on success
      setContent('');

      // Notify parent component
      onPostCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đã có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setContent('');
    setError(null);
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">
        Tạo bài viết
      </h2>

      <form onSubmit={handleSubmit}>
        {/* Textarea */}
        <div className="mb-4">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Bạn đang nghĩ gì về thị trường bất động sản?"
            rows={4}
            className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none ${
              isOverLimit ? 'border-red-500' : 'border-gray-300'
            }`}
            disabled={isLoading}
          />

          {/* Character Counter */}
          <div className="flex justify-end mt-2">
            <span
              className={`text-sm ${
                isOverLimit
                  ? 'text-red-600 font-semibold'
                  : characterCount > MAX_CHARACTERS * 0.9
                  ? 'text-orange-600'
                  : 'text-gray-500'
              }`}
            >
              {characterCount}/{MAX_CHARACTERS}
            </span>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-4 bg-red-50 text-red-800 p-4 rounded-md text-sm">
            {error}
          </div>
        )}

        {/* Buttons */}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={!isValid || isLoading}
            className="px-6 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors duration-200"
          >
            {isLoading ? 'Đang đăng...' : 'Đăng'}
          </button>

          {content && !isLoading && (
            <button
              type="button"
              onClick={handleCancel}
              className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition-colors duration-200"
            >
              Hủy
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
