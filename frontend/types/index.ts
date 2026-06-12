export type PropertyType = 'house' | 'apartment' | 'villa' | 'townhouse';
export type PropertyStatus = 'for-sale' | 'for-rent';
export type GeocodingStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'SKIPPED';

export interface Property {
  id: string;
  title: string;
  description: string;
  price: number;
  address: string;
  city: string;
  bedrooms: number;
  bathrooms: number;
  area: number;
  propertyType: PropertyType;
  status: PropertyStatus;
  images: string[];
  createdAt: string;
  userId: string;
  features: string[];
  latitude: number | null;
  longitude: number | null;
  geocodingStatus: GeocodingStatus;
}

export interface User {
  id: string;
  name: string;
  email: string;
  password: string;
}

export interface SearchFilters {
  query?: string;
  city?: string;
  propertyType?: PropertyType;
  status?: PropertyStatus;
  minPrice?: number;
  maxPrice?: number;
  bedrooms?: number;
}

export interface PaginationParams {
  page: number;
  perPage: number;
}

export interface PaginatedResult<T> {
  data: T[];
  total: number;
  page: number;
  perPage: number;
  totalPages: number;
}

// Legacy Post type (for mock data compatibility)
export interface LegacyPost {
  id: string;
  content: string;
  userId: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Like {
  id: string;
  postId: string;
  userId: string;
  createdAt: string;
}

// Legacy PostWithMetadata (for mock data compatibility)
export interface LegacyPostWithMetadata extends LegacyPost {
  user: {
    id: string;
    name: string;
    email: string;
  };
  likeCount: number;
  isLikedByCurrentUser: boolean;
  likes: Like[];
}

// New Post types (from post-service API)
export interface PostAuthor {
  id: string;
  name: string | null;
  email: string | null;
}

export interface Post {
  id: string;
  content: string | null;
  imageUrls: string[];
  author: PostAuthor;
  likeCount: number;
  isLikedByCurrentUser: boolean;
  createdAt: string;
  updatedAt: string;
}

// Alias for compatibility
export type PostWithMetadata = Post;
