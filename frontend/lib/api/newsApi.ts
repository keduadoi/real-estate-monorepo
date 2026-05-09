import {
  CreateNewsArticleRequest,
  NewsArticleResponse,
  NewsListResponse,
  UpdateNewsArticleRequest,
} from '@/types/api';

const API_BASE_URL = process.env.NEXT_PUBLIC_KONG_URL || 'http://127.0.0.1:8000';

interface UserInfo {
  id: string;
  email?: string | null;
  name?: string | null;
  roles?: string[];
}

interface RequestOptions {
  accessToken?: string;
  user?: UserInfo;
}

class NewsApi {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  private buildHeaders(options?: RequestOptions, contentType?: string): HeadersInit {
    const headers: HeadersInit = {};
    if (contentType) headers['Content-Type'] = contentType;
    if (options?.accessToken) headers['Authorization'] = `Bearer ${options.accessToken}`;

    if (options?.user) {
      headers['X-User-Id'] = options.user.id;
      if (options.user.email) headers['X-User-Email'] = options.user.email;
      if (options.user.name) headers['X-User-Name'] = options.user.name;
      if (options.user.roles?.length) {
        headers['X-User-Roles'] = options.user.roles.join(',');
      }
    }

    return headers;
  }

  async list(page = 0, perPage = 20, category?: string): Promise<NewsListResponse> {
    const params = new URLSearchParams({
      page: String(page),
      perPage: String(perPage),
    });
    if (category) params.set('category', category);

    const response = await fetch(`${this.baseUrl}/api/news?${params.toString()}`, {
      cache: 'no-store',
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch news: ${response.statusText}`);
    }
    return response.json();
  }

  async getById(id: number): Promise<NewsArticleResponse> {
    const response = await fetch(`${this.baseUrl}/api/news/${id}`, {
      cache: 'no-store',
    });
    if (!response.ok) {
      if (response.status === 404) throw new Error('News article not found');
      throw new Error(`Failed to fetch news article: ${response.statusText}`);
    }
    return response.json();
  }

  async create(data: CreateNewsArticleRequest, options?: RequestOptions): Promise<NewsArticleResponse> {
    const response = await fetch(`${this.baseUrl}/api/news`, {
      method: 'POST',
      headers: this.buildHeaders(options, 'application/json'),
      body: JSON.stringify(data),
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: response.statusText }));
      throw new Error(error.message || 'Failed to create news article');
    }
    return response.json();
  }

  async update(
    id: number,
    data: UpdateNewsArticleRequest,
    options?: RequestOptions
  ): Promise<NewsArticleResponse> {
    const response = await fetch(`${this.baseUrl}/api/news/${id}`, {
      method: 'PUT',
      headers: this.buildHeaders(options, 'application/json'),
      body: JSON.stringify(data),
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: response.statusText }));
      throw new Error(error.message || 'Failed to update news article');
    }
    return response.json();
  }

  async remove(id: number, options?: RequestOptions): Promise<void> {
    const response = await fetch(`${this.baseUrl}/api/news/${id}`, {
      method: 'DELETE',
      headers: this.buildHeaders(options),
    });
    if (!response.ok && response.status !== 204) {
      const error = await response.json().catch(() => ({ message: response.statusText }));
      throw new Error(error.message || 'Failed to delete news article');
    }
  }
}

export const newsApi = new NewsApi();
export default newsApi;
