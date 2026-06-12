'use client';

import { useEffect, useState } from 'react';
import Image from 'next/image';
import { useTranslations } from 'next-intl';
import { fixImageUrl } from '@/lib/utils';

interface ImageLightboxProps {
  images: string[];
  initialIndex: number;
  title: string;
  onClose: () => void;
}

export default function ImageLightbox({
  images,
  initialIndex,
  title,
  onClose,
}: ImageLightboxProps) {
  const t = useTranslations('components.imageGallery');
  const [selectedIndex, setSelectedIndex] = useState(initialIndex);

  const goToPrevious = () => {
    setSelectedIndex((prev) => (prev === 0 ? images.length - 1 : prev - 1));
  };

  const goToNext = () => {
    setSelectedIndex((prev) => (prev === images.length - 1 ? 0 : prev + 1));
  };

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      } else if (event.key === 'ArrowLeft') {
        setSelectedIndex((prev) => (prev === 0 ? images.length - 1 : prev - 1));
      } else if (event.key === 'ArrowRight') {
        setSelectedIndex((prev) => (prev === images.length - 1 ? 0 : prev + 1));
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [images.length, onClose]);

  return (
    <div
      className="fixed inset-0 z-50 bg-black/95 flex items-center justify-center"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label={t('altImage', { title, index: selectedIndex + 1 })}
    >
      <div
        className="relative w-full h-full"
        onClick={(e) => e.stopPropagation()}
      >
        <Image
          src={fixImageUrl(images[selectedIndex])}
          alt={t('altImage', { title, index: selectedIndex + 1 })}
          fill
          className="object-contain"
          sizes="100vw"
          priority
        />
      </div>

      <button
        onClick={onClose}
        className="absolute top-4 right-4 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full"
        aria-label={t('closeFullscreen')}
      >
        <svg
          className="w-6 h-6"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M6 18L18 6M6 6l12 12"
          />
        </svg>
      </button>

      {images.length > 1 && (
        <>
          <button
            onClick={(e) => {
              e.stopPropagation();
              goToPrevious();
            }}
            className="absolute left-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full"
            aria-label={t('previous')}
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 19l-7-7 7-7"
              />
            </svg>
          </button>

          <button
            onClick={(e) => {
              e.stopPropagation();
              goToNext();
            }}
            className="absolute right-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full"
            aria-label={t('next')}
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 5l7 7-7 7"
              />
            </svg>
          </button>

          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 bg-black/50 text-white px-3 py-1 rounded-full text-sm">
            {selectedIndex + 1} / {images.length}
          </div>
        </>
      )}
    </div>
  );
}
