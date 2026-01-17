import { Property, SearchFilters, PaginatedResult, PaginationParams } from '@/types';

/**
 * Paginate an array of items
 */
export function paginateArray<T>(
  items: T[],
  params: PaginationParams
): PaginatedResult<T> {
  const { page, perPage } = params;
  const total = items.length;
  const totalPages = Math.ceil(total / perPage);
  const startIndex = (page - 1) * perPage;
  const endIndex = startIndex + perPage;
  const data = items.slice(startIndex, endIndex);

  return {
    data,
    total,
    page,
    perPage,
    totalPages,
  };
}

/**
 * Filter properties based on search criteria
 */
export function filterProperties(
  properties: Property[],
  filters: SearchFilters
): Property[] {
  return properties.filter((property) => {
    // Text query filter (searches title, description, address)
    if (filters.query) {
      const query = filters.query.toLowerCase();
      const matchesQuery =
        property.title.toLowerCase().includes(query) ||
        property.description.toLowerCase().includes(query) ||
        property.address.toLowerCase().includes(query);

      if (!matchesQuery) return false;
    }

    // City filter
    if (filters.city && property.city !== filters.city) {
      return false;
    }

    // Property type filter
    if (filters.propertyType && property.propertyType !== filters.propertyType) {
      return false;
    }

    // Status filter
    if (filters.status && property.status !== filters.status) {
      return false;
    }

    // Min price filter
    if (filters.minPrice && property.price < filters.minPrice) {
      return false;
    }

    // Max price filter
    if (filters.maxPrice && property.price > filters.maxPrice) {
      return false;
    }

    // Bedrooms filter
    if (filters.bedrooms && property.bedrooms < filters.bedrooms) {
      return false;
    }

    return true;
  });
}

/**
 * Format price in Vietnamese Dong (VND)
 */
export function formatPrice(price: number): string {
  if (price >= 1000000000) {
    // Billion (tỷ)
    const billions = price / 1000000000;
    return `${billions.toFixed(1)} tỷ`;
  } else if (price >= 1000000) {
    // Million (triệu)
    const millions = price / 1000000;
    return `${millions.toFixed(0)} triệu`;
  } else {
    // Less than a million
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  }
}

/**
 * Format area in square meters
 */
export function formatArea(area: number): string {
  return `${area} m²`;
}

/**
 * Sort properties by various criteria
 */
export type SortOption = 'newest' | 'oldest' | 'price-low' | 'price-high';

export function sortProperties(
  properties: Property[],
  sortBy?: SortOption
): Property[] {
  if (!sortBy) {
    // Default: newest first
    return [...properties].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  }

  const sorted = [...properties];

  switch (sortBy) {
    case 'newest':
      return sorted.sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    case 'oldest':
      return sorted.sort(
        (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      );
    case 'price-low':
      return sorted.sort((a, b) => a.price - b.price);
    case 'price-high':
      return sorted.sort((a, b) => b.price - a.price);
    default:
      return sorted;
  }
}

/**
 * Combine multiple class names
 */
export function cn(...classes: (string | undefined | null | false)[]): string {
  return classes.filter(Boolean).join(' ');
}
