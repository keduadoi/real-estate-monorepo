'use client';

import { useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { useTranslations } from 'next-intl';
import Image from 'next/image';
import { postApi } from '@/lib/api/postApi';
import { useImageUpload } from '@/hooks/useImageUpload';

interface PostFormProps {
  onPostCreated: () => void;
}

const MAX_CHARACTERS = 5000;
const MAX_IMAGES = 10;

export default function PostForm({ onPostCreated }: PostFormProps) {
  const { data: session } = useSession();
  const t = useTranslations('components.postForm');
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const imageUpload = useImageUpload();
  const { images, uploading, addFiles, removeFile, uploadImages, clearAll } = imageUpload;

  const characterCount = content.length;
  const isOverLimit = characterCount > MAX_CHARACTERS;
  const isValid = (content.trim().length > 0 || images.length > 0) && !isOverLimit;
  const canAddMore = images.length < MAX_IMAGES;

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(e.target.files);
      e.target.value = '';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isValid) return;

    const accessToken = session?.accessToken as string | undefined;
    if (!accessToken) {
      setError(t('loginRequired'));
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      let imageUrls: string[] = [];
      if (images.length > 0) {
        try {
          imageUrls = await uploadImages();
        } catch {
          setError(t('uploadFailed'));
          return;
        }
      }

      await postApi.create(
        {
          content: content.trim() || undefined,
          imageUrls: imageUrls.length > 0 ? imageUrls : undefined,
        },
        {
          accessToken,
          user: session?.user ? {
            id: session.user.id as string,
            email: session.user.email,
            name: session.user.name,
            roles: ['ROLE_USER']
          } : undefined
        }
      );

      setContent('');
      clearAll();
      onPostCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : t('generalError'));
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setContent('');
    clearAll();
    setError(null);
  };

  const isBusy = isLoading || uploading;

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">
        {t('heading')}
      </h2>

      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={t('placeholder')}
            rows={4}
            className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none ${
              isOverLimit ? 'border-red-500' : 'border-gray-300'
            }`}
            disabled={isBusy}
          />

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

        {images.length > 0 && (
          <div className="mb-4 flex flex-wrap gap-2">
            {images.map((image) => (
              <div key={image.id} className="relative w-20 h-20 rounded-lg overflow-hidden border border-gray-200">
                <Image
                  src={image.preview}
                  alt={image.file.name}
                  fill
                  className="object-cover"
                  sizes="80px"
                  unoptimized
                />
                <button
                  type="button"
                  onClick={() => removeFile(image.id)}
                  disabled={isBusy}
                  className="absolute top-1 right-1 bg-black/60 hover:bg-black/80 text-white rounded-full p-0.5"
                  aria-label={t('removeImage')}
                >
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            ))}
          </div>
        )}

        {(error || imageUpload.error) && (
          <div className="mb-4 bg-red-50 text-red-800 p-4 rounded-md text-sm">
            {error || imageUpload.error}
          </div>
        )}

        <div className="flex items-center gap-3">
          <button
            type="submit"
            disabled={!isValid || isBusy}
            className="px-6 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors duration-200"
          >
            {isBusy ? t('submitting') : t('submit')}
          </button>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            onChange={handleFileInput}
            className="hidden"
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={isBusy || !canAddMore}
            className="flex items-center gap-2 px-4 py-2 text-gray-700 rounded-lg font-medium hover:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed transition-colors duration-200"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            {t('addPhotos')}
            {images.length > 0 && (
              <span className="text-sm text-gray-500">
                {t('imageCounter', { current: images.length, max: MAX_IMAGES })}
              </span>
            )}
          </button>

          {(content || images.length > 0) && !isBusy && (
            <button
              type="button"
              onClick={handleCancel}
              className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition-colors duration-200"
            >
              {t('cancel')}
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
