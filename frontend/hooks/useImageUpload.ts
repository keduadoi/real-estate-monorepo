import { useState, useCallback } from 'react';

const MAX_FILE_SIZE = parseInt(process.env.NEXT_PUBLIC_MAX_IMAGE_SIZE || '10485760'); // 10MB
const MAX_IMAGES = parseInt(process.env.NEXT_PUBLIC_MAX_IMAGES_PER_PROPERTY || '10');
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

interface ImageFile {
  file: File;
  preview: string;
  id: string;
}

interface UseImageUploadReturn {
  images: ImageFile[];
  uploading: boolean;
  progress: number;
  error: string | null;
  addFiles: (files: FileList | File[]) => void;
  removeFile: (id: string) => void;
  uploadImages: (propertyId?: number) => Promise<string[]>;
  clearAll: () => void;
  resetError: () => void;
}

export function useImageUpload(): UseImageUploadReturn {
  const [images, setImages] = useState<ImageFile[]>([]);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const validateFile = useCallback((file: File): string | null => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return `Invalid file type: ${file.name}. Allowed types: JPEG, PNG, WebP`;
    }
    if (file.size > MAX_FILE_SIZE) {
      return `File too large: ${file.name}. Maximum size: ${MAX_FILE_SIZE / (1024 * 1024)}MB`;
    }
    return null;
  }, []);

  const addFiles = useCallback(
    (files: FileList | File[]) => {
      setError(null);
      const fileArray = Array.from(files);

      if (images.length + fileArray.length > MAX_IMAGES) {
        setError(`Cannot upload more than ${MAX_IMAGES} images`);
        return;
      }

      const newImages: ImageFile[] = [];
      const errors: string[] = [];

      fileArray.forEach((file) => {
        const validationError = validateFile(file);
        if (validationError) {
          errors.push(validationError);
        } else {
          const id = `${Date.now()}-${Math.random()}`;
          const preview = URL.createObjectURL(file);
          newImages.push({ file, preview, id });
        }
      });

      if (errors.length > 0) {
        setError(errors.join('; '));
      }

      if (newImages.length > 0) {
        setImages((prev) => [...prev, ...newImages]);
      }
    },
    [images.length, validateFile]
  );

  const removeFile = useCallback((id: string) => {
    setImages((prev) => {
      const image = prev.find((img) => img.id === id);
      if (image) {
        URL.revokeObjectURL(image.preview);
      }
      return prev.filter((img) => img.id !== id);
    });
    setError(null);
  }, []);

  const uploadImages = useCallback(
    async (propertyId?: number): Promise<string[]> => {
      if (images.length === 0) {
        return [];
      }

      setUploading(true);
      setProgress(0);
      setError(null);

      try {
        const formData = new FormData();
        images.forEach((img) => {
          formData.append('files', img.file);
        });

        // Use local API route for temp uploads (with backend fallback)
        // For property-specific uploads, use the backend API directly
        const backendApiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
        const endpoint = propertyId
          ? `${backendApiUrl}/properties/${propertyId}/images`
          : '/api/upload/temp';

        const response = await fetch(endpoint, {
          method: 'POST',
          body: formData,
        });

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({}));
          throw new Error(errorData.message || 'Failed to upload images');
        }

        const data = await response.json();
        setProgress(100);

        // Clean up blob URLs
        images.forEach((img) => URL.revokeObjectURL(img.preview));

        return data.imageUrls || [];
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to upload images';
        setError(errorMessage);
        throw err;
      } finally {
        setUploading(false);
      }
    },
    [images]
  );

  const clearAll = useCallback(() => {
    images.forEach((img) => URL.revokeObjectURL(img.preview));
    setImages([]);
    setError(null);
    setProgress(0);
  }, [images]);

  const resetError = useCallback(() => {
    setError(null);
  }, []);

  return {
    images,
    uploading,
    progress,
    error,
    addFiles,
    removeFile,
    uploadImages,
    clearAll,
    resetError,
  };
}
