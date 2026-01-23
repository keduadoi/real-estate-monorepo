'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useSession } from 'next-auth/react';
import DeleteConfirmDialog from './DeleteConfirmDialog';

interface PropertyActionButtonsProps {
  propertyId: string;
  propertyUserId: string;
  propertyTitle: string;
}

export default function PropertyActionButtons({
  propertyId,
  propertyUserId,
  propertyTitle,
}: PropertyActionButtonsProps) {
  const { data: session } = useSession();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  // Only show buttons if user is logged in and is the owner
  const isOwner = session?.user?.id === propertyUserId;

  if (!isOwner) {
    return null;
  }

  return (
    <>
      <div className="flex gap-3">
        <Link
          href={`/properties/${propertyId}/edit`}
          className="inline-flex items-center px-4 py-2 border border-primary-600 text-primary-600 rounded-md hover:bg-primary-50 transition-colors font-medium"
        >
          <svg
            className="w-4 h-4 mr-2"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
            />
          </svg>
          Sửa tin
        </Link>
        <button
          onClick={() => setShowDeleteDialog(true)}
          className="inline-flex items-center px-4 py-2 border border-red-600 text-red-600 rounded-md hover:bg-red-50 transition-colors font-medium"
        >
          <svg
            className="w-4 h-4 mr-2"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
          Xóa tin
        </button>
      </div>

      <DeleteConfirmDialog
        isOpen={showDeleteDialog}
        onClose={() => setShowDeleteDialog(false)}
        propertyId={propertyId}
        propertyTitle={propertyTitle}
      />
    </>
  );
}
