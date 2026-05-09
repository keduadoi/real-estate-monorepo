'use client';

import Link from 'next/link';
import { useSearchParams, usePathname } from 'next/navigation';
import { useTranslations } from 'next-intl';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
}

export default function Pagination({ currentPage, totalPages }: PaginationProps) {
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const t = useTranslations('components.pagination');

  const createPageURL = (pageNumber: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('page', pageNumber.toString());
    return `${pathname}?${params.toString()}`;
  };

  if (totalPages <= 1) {
    return null;
  }

  const pages: (number | string)[] = [];
  const showEllipsisStart = currentPage > 3;
  const showEllipsisEnd = currentPage < totalPages - 2;

  pages.push(1);

  if (showEllipsisStart) {
    pages.push('...');
  }

  for (let i = Math.max(2, currentPage - 1); i <= Math.min(totalPages - 1, currentPage + 1); i++) {
    if (i !== 1 && i !== totalPages) {
      pages.push(i);
    }
  }

  if (showEllipsisEnd) {
    pages.push('...');
  }

  if (totalPages > 1) {
    pages.push(totalPages);
  }

  return (
    <div className="flex items-center justify-center space-x-2 my-8">
      {currentPage > 1 ? (
        <Link
          href={createPageURL(currentPage - 1)}
          className="px-3 py-2 rounded-md border border-gray-300 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          {t('previous')}
        </Link>
      ) : (
        <span className="px-3 py-2 rounded-md border border-gray-200 text-sm font-medium text-gray-400 cursor-not-allowed">
          {t('previous')}
        </span>
      )}

      <div className="hidden sm:flex space-x-2">
        {pages.map((page, index) => {
          if (page === '...') {
            return (
              <span key={`ellipsis-${index}`} className="px-3 py-2 text-gray-500">
                ...
              </span>
            );
          }

          const pageNumber = page as number;
          const isActive = pageNumber === currentPage;

          return (
            <Link
              key={pageNumber}
              href={createPageURL(pageNumber)}
              className={`px-3 py-2 rounded-md text-sm font-medium ${
                isActive
                  ? 'bg-primary-600 text-white'
                  : 'border border-gray-300 text-gray-700 hover:bg-gray-50'
              }`}
            >
              {pageNumber}
            </Link>
          );
        })}
      </div>

      <div className="sm:hidden px-3 py-2 text-sm text-gray-700">
        {t('page', { current: currentPage, total: totalPages })}
      </div>

      {currentPage < totalPages ? (
        <Link
          href={createPageURL(currentPage + 1)}
          className="px-3 py-2 rounded-md border border-gray-300 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          {t('next')}
        </Link>
      ) : (
        <span className="px-3 py-2 rounded-md border border-gray-200 text-sm font-medium text-gray-400 cursor-not-allowed">
          {t('next')}
        </span>
      )}
    </div>
  );
}
