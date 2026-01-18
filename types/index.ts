export type PropertyType = 'house' | 'apartment' | 'villa' | 'townhouse';
export type PropertyStatus = 'for-sale' | 'for-rent';

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

export interface Post {
  id: string;                    // "post-1", "post-2", etc.
  content: string;               // Post text content
  userId: string;                // Creator's user ID
  createdAt: string;             // ISO timestamp
  updatedAt?: string;            // ISO timestamp (for edits - future)
}

export interface Like {
  id: string;                    // "like-{timestamp}"
  postId: string;                // Associated post ID
  userId: string;                // User who liked
  createdAt: string;             // ISO timestamp
}

export interface PostWithMetadata extends Post {
  user: {                        // Author information
    id: string;
    name: string;
    email: string;
  };
  likeCount: number;             // Total likes
  isLikedByCurrentUser: boolean; // Current user's like status
  likes: Like[];                 // All likes (for display)
}
