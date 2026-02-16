import {
  PriceResponse,
  PriceHistoryResponse,
  UpdatePriceRequest,
} from '@/types/api';

const API_BASE_URL = process.env.NEXT_PUBLIC_KONG_URL || 'http://127.0.0.1:8000';

interface RequestOptions {
  accessToken?: string;
}

class PriceApi {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  private buildHeaders(options?: RequestOptions, contentType?: string): HeadersInit {
    const headers: HeadersInit = {};

    if (contentType) {
      headers['Content-Type'] = contentType;
    }

    if (options?.accessToken) {
      headers['Authorization'] = `Bearer ${options.accessToken}`;
    }

    return headers;
  }

  async getCurrentPrice(propertyId: number): Promise<PriceResponse> {
    const response = await fetch(`${this.baseUrl}/api/prices/${propertyId}`, {
      cache: 'no-store',
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Price not found');
      }
      throw new Error(`Failed to fetch price: ${response.statusText}`);
    }

    return response.json();
  }

  async getPriceHistory(propertyId: number): Promise<PriceHistoryResponse> {
    const response = await fetch(`${this.baseUrl}/api/prices/${propertyId}/history`, {
      cache: 'no-store',
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Price history not found');
      }
      throw new Error(`Failed to fetch price history: ${response.statusText}`);
    }

    return response.json();
  }

  async updatePrice(
    propertyId: number,
    data: UpdatePriceRequest,
    options?: RequestOptions
  ): Promise<PriceResponse> {
    const response = await fetch(`${this.baseUrl}/api/prices/${propertyId}`, {
      method: 'PUT',
      headers: this.buildHeaders(options, 'application/json'),
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('Authentication required');
      }
      const error = await response.json().catch(() => ({ message: response.statusText }));
      throw new Error(error.message || 'Failed to update price');
    }

    return response.json();
  }
}

export const priceApi = new PriceApi();
export default priceApi;
