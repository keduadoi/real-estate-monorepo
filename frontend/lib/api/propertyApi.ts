import {
  Property,
  PropertySearchRequest,
  PageResponse,
  CreatePropertyRequest,
  UpdatePropertyRequest,
} from '@/types/api';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

/**
 * API client for property endpoints
 */
class PropertyApi {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  /**
   * Get all properties with pagination
   */
  async getAll(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'createdAt',
    sortDirection: 'asc' | 'desc' = 'desc'
  ): Promise<PageResponse<Property>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy,
      sortDirection,
    });

    const response = await fetch(`${this.baseUrl}/properties?${params}`, {
      cache: 'no-store',
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch properties: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Search and filter properties
   */
  async search(
    searchRequest: Partial<PropertySearchRequest>,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<Property>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    const response = await fetch(`${this.baseUrl}/properties/search?${params}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(searchRequest),
      cache: 'no-store',
    });

    if (!response.ok) {
      throw new Error(`Failed to search properties: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get property by ID
   */
  async getById(id: number): Promise<Property> {
    const response = await fetch(`${this.baseUrl}/properties/${id}`, {
      cache: 'no-store',
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Property not found');
      }
      throw new Error(`Failed to fetch property: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get properties by user ID
   */
  async getByUserId(
    userId: string,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<Property>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    const response = await fetch(`${this.baseUrl}/properties/user/${userId}?${params}`, {
      cache: 'no-store',
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch user properties: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get distinct cities
   */
  async getCities(): Promise<string[]> {
    const response = await fetch(`${this.baseUrl}/properties/cities`, {
      cache: 'no-store',
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch cities: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Create new property
   */
  async create(propertyData: CreatePropertyRequest): Promise<Property> {
    const response = await fetch(`${this.baseUrl}/properties`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(propertyData),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: response.statusText }));
      throw new Error(error.message || 'Failed to create property');
    }

    return response.json();
  }

  /**
   * Update existing property
   */
  async update(id: number, propertyData: Partial<UpdatePropertyRequest>): Promise<Property> {
    const response = await fetch(`${this.baseUrl}/properties/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(propertyData),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: response.statusText }));
      throw new Error(error.message || 'Failed to update property');
    }

    return response.json();
  }

  /**
   * Delete property
   */
  async delete(id: number): Promise<void> {
    const response = await fetch(`${this.baseUrl}/properties/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error(`Failed to delete property: ${response.statusText}`);
    }
  }

  /**
   * Check if property exists
   */
  async exists(id: number): Promise<boolean> {
    const response = await fetch(`${this.baseUrl}/properties/${id}`, {
      method: 'HEAD',
    });

    return response.ok;
  }

  /**
   * Count properties by status
   */
  async countByStatus(status: 'FOR_SALE' | 'FOR_RENT'): Promise<number> {
    const response = await fetch(`${this.baseUrl}/properties/count/${status}`, {
      cache: 'no-store',
    });

    if (!response.ok) {
      throw new Error(`Failed to count properties: ${response.statusText}`);
    }

    return response.json();
  }
}

// Export singleton instance
export const propertyApi = new PropertyApi();
export default propertyApi;
