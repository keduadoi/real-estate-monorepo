// Backend API types that match the Java DTOs

export type PropertyType = 'HOUSE' | 'APARTMENT' | 'VILLA' | 'TOWNHOUSE';
export type PropertyStatus = 'FOR_SALE' | 'FOR_RENT';

export interface Property {
  id: number;
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
  features: string[];
  userId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePropertyRequest {
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
  images?: string[];
  features?: string[];
  userId: string;
}

export interface UpdatePropertyRequest {
  title?: string;
  description?: string;
  price?: number;
  address?: string;
  city?: string;
  bedrooms?: number;
  bathrooms?: number;
  area?: number;
  propertyType?: PropertyType;
  status?: PropertyStatus;
  images?: string[];
  features?: string[];
}

export interface PropertySearchRequest {
  query?: string;
  city?: string;
  propertyType?: PropertyType;
  status?: PropertyStatus;
  minPrice?: number;
  maxPrice?: number;
  bedrooms?: number;
  userId?: string;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

export interface PageResponse<T> {
  data: T[];
  total: number;
  page: number;
  perPage: number;
  totalPages: number;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface ValidationErrorResponse extends ErrorResponse {
  validationErrors: Record<string, string>;
}

export interface ImageUploadResponse {
  imageUrls: string[];
  totalUploaded: number;
}

export interface ImageDeleteRequest {
  imageUrls: string[];
}

// Price Service types

export interface PriceResponse {
  propertyId: number;
  currentPrice: number;
  currency: string;
  lastUpdatedBy: string;
  updatedAt: string;
}

export interface PricePointResponse {
  oldPrice: number | null;
  newPrice: number;
  changedBy: string;
  changedAt: string;
  reason: string;
}

export interface PriceHistoryResponse {
  propertyId: number;
  pricePoints: PricePointResponse[];
}

export interface UpdatePriceRequest {
  newPrice: number;
  currency?: string;
  reason?: string;
}

// News Service types

export interface NewsArticleSummary {
  id: number;
  title: string;
  summary: string;
  author: string | null;
  category: string | null;
  imageUrl: string | null;
  publishedAt: string | null;
}

export interface NewsArticleResponse {
  id: number;
  title: string;
  summary: string;
  content: string;
  author: string | null;
  category: string | null;
  imageUrl: string | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface NewsListResponse {
  data: NewsArticleSummary[];
  total: number;
  page: number;
  perPage: number;
  totalPages: number;
}

export interface CreateNewsArticleRequest {
  title: string;
  summary: string;
  content: string;
  author?: string;
  category?: string;
  imageUrl?: string;
  publishedAt?: string;
}

export interface UpdateNewsArticleRequest {
  title?: string;
  summary?: string;
  content?: string;
  author?: string;
  category?: string;
  imageUrl?: string;
  publishedAt?: string;
}
