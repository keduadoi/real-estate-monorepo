'use client';

import Link from 'next/link';
import { useSearchParams, usePathname } from 'next/navigation';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
}

export default function Pagination({ currentPage, totalPages }: PaginationProps) {
  const searchParams = useSearchParams();
  const pathname = usePathname();

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

  // Always show first page
  pages.push(1);

  // Show ellipsis or pages before current
  if (showEllipsisStart) {
    pages.push('...');
  }

  // Show pages around current page
  for (let i = Math.max(2, currentPage - 1); i <= Math.min(totalPages - 1, currentPage + 1); i++) {
    if (i !== 1 && i !== totalPages) {
      pages.push(i);
    }
  }

  // Show ellipsis or pages after current
  if (showEllipsisEnd) {
    pages.push('...');
  }

  // Always show last page (if there's more than 1 page)
  if (totalPages > 1) {
    pages.push(totalPages);
  }

  return (
    <div className="flex items-center justify-center space-x-2 my-8">
      {/* Previous button */}
      {currentPage > 1 ? (
        <Link
          href={createPageURL(currentPage - 1)}
          className="px-3 py-2 rounded-md border border-gray-300 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          ← Trước
        </Link>
      ) : (
        <span className="px-3 py-2 rounded-md border border-gray-200 text-sm font-medium text-gray-400 cursor-not-allowed">
          ← Trước
        </span>
      )}

      {/* Page numbers */}
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

      {/* Mobile page indicator */}
      <div className="sm:hidden px-3 py-2 text-sm text-gray-700">
        Trang {currentPage} / {totalPages}
      </div>

      {/* Next button */}
      {currentPage < totalPages ? (
        <Link
          href={createPageURL(currentPage + 1)}
          className="px-3 py-2 rounded-md border border-gray-300 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Tiếp →
        </Link>
      ) : (
        <span className="px-3 py-2 rounded-md border border-gray-200 text-sm font-medium text-gray-400 cursor-not-allowed">
          Tiếp →
        </span>
      )}
    </div>
  );
}
