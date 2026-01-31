// Use local API routes for upload (with backend fallback)
const getApiUrl = () => {
  if (typeof window === 'undefined') {
    return process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000';
  }
  return '';
};

// For property-specific endpoints that need to go through Kong
const BACKEND_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export interface ImageUploadResponse {
  imageUrls: string[];
  totalUploaded: number;
}

export interface ImageDeleteRequest {
  imageUrls: string[];
}

/**
 * Upload temporary images before property creation
 * Uses local API route which proxies to backend or falls back to local storage
 */
export async function uploadTempImages(files: File[]): Promise<string[]> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await fetch(`${getApiUrl()}/api/upload/temp`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || 'Failed to upload images');
  }

  const data: ImageUploadResponse = await response.json();
  return data.imageUrls;
}

/**
 * Upload images for an existing property
 */
export async function uploadForProperty(
  propertyId: number,
  files: File[]
): Promise<string[]> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await fetch(`${API_URL}/properties/${propertyId}/images`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || 'Failed to upload images');
  }

  const data: ImageUploadResponse = await response.json();
  return data.imageUrls;
}

/**
 * Delete images from a property
 */
export async function deleteFromProperty(
  propertyId: number,
  imageUrls: string[]
): Promise<void> {
  const response = await fetch(`${API_URL}/properties/${propertyId}/images`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ imageUrls }),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || 'Failed to delete images');
  }
}
