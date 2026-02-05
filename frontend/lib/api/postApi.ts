/**
 * Post Service API Client
 *
 * Handles all communication with the post-service backend
 * for social feed functionality (posts, likes).
 *
 * Calls Kong Gateway directly with Bearer token authentication.
 */

// Kong Gateway URL for post-service
const getApiBaseUrl = () => {
  // Use Kong Gateway URL from environment or default
  return process.env.NEXT_PUBLIC_KONG_URL || 'http://127.0.0.1:8000';
};

const API_BASE_URL = getApiBaseUrl();

// ============================================================================
// TYPES
// ============================================================================

export interface PostAuthor {
  id: string;
  name: string | null;
  email: string | null;
}

export interface Post {
  id: string;
  content: string;
  author: PostAuthor;
  likeCount: number;
  isLikedByCurrentUser: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostRequest {
  content: string;
}

export interface UpdatePostRequest {
  content: string;
}

export interface LikeResponse {
  liked: boolean;
  likeCount: number;
}

export interface PageResponse<T> {
  data: T[];
  total: number;
  page: number;
  perPage: number;
  totalPages: number;
}

export interface UserInfo {
  id: string;
  email?: string | null;
  name?: string | null;
  roles?: string[];
}

export interface RequestOptions {
  accessToken?: string;
  user?: UserInfo;
}

// ============================================================================
// API CLIENT
// ============================================================================

/**
 * API client for post service endpoints.
 * Supports both public and authenticated requests through Kong Gateway.
 */
class PostApi {
  private getBaseUrl(): string {
    return getApiBaseUrl();
  }

  /**
   * Build headers for requests
   * Includes X-User-* headers for microservice authentication
   */
  private buildHeaders(options?: RequestOptions, contentType?: string): HeadersInit {
    const headers: HeadersInit = {};

    if (contentType) {
      headers['Content-Type'] = contentType;
    }

    if (options?.accessToken) {
      headers['Authorization'] = `Bearer ${options.accessToken}`;
    }

    // Add X-User-* headers for microservice authentication
    if (options?.user) {
      headers['X-User-Id'] = options.user.id;
      if (options.user.email) {
        headers['X-User-Email'] = options.user.email;
      }
      if (options.user.name) {
        headers['X-User-Name'] = options.user.name;
      }
      if (options.user.roles?.length) {
        headers['X-User-Roles'] = options.user.roles.join(',');
      }
    }

    return headers;
  }

  /**
   * Handle API errors
   */
  private async handleError(response: Response): Promise<never> {
    if (response.status === 401) {
      throw new Error('Authentication required');
    }
    if (response.status === 403) {
      throw new Error('You do not have permission to perform this action');
    }
    if (response.status === 404) {
      throw new Error('Post not found');
    }

    try {
      const error = await response.json();
      throw new Error(error.message || response.statusText);
    } catch {
      throw new Error(response.statusText || 'An error occurred');
    }
  }

  // ==========================================================================
  // PUBLIC ENDPOINTS
  // ==========================================================================

  /**
   * Get paginated feed of all posts (newest first).
   * Authentication is optional - if provided, shows like status.
   */
  async getFeed(
    page: number = 0,
    size: number = 20,
    options?: RequestOptions
  ): Promise<PageResponse<Post>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      _t: Date.now().toString(), // Cache-busting timestamp
    });

    const response = await fetch(`${this.getBaseUrl()}/api/posts?${params}`, {
      cache: 'no-store',
      headers: this.buildHeaders(options),
    });

    if (!response.ok) {
      await this.handleError(response);
    }

    return response.json();
  }

  /**
   * Get a single post by ID.
   * Authentication is optional - if provided, shows like status.
   */
  async getById(id: string, options?: RequestOptions): Promise<Post> {
    const response = await fetch(`${this.getBaseUrl()}/api/posts/${id}`, {
      cache: 'no-store',
      headers: this.buildHeaders(options),
    });

    if (!response.ok) {
      await this.handleError(response);
    }

    return response.json();
  }

  /**
   * Get posts by a specific user.
   * Authentication is optional - if provided, shows like status.
   */
  async getByUserId(
    userId: string,
    page: number = 0,
    size: number = 20,
    options?: RequestOptions
  ): Promise<PageResponse<Post>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    const response = await fetch(`${this.getBaseUrl()}/api/posts/user/${userId}?${params}`, {
      cache: 'no-store',
      headers: this.buildHeaders(options),
    });

    if (!response.ok) {
      await this.handleError(response);
    }

    return response.json();
  }

  /**
   * Search posts by content.
   */
  async search(
    query: string,
    page: number = 0,
    size: number = 20,
    options?: RequestOptions
  ): Promise<PageResponse<Post>> {
    const params = new URLSearchParams({
      q: query,
      page: page.toString(),
      size: size.toString(),
    });

    const response = await fetch(`${this.getBaseUrl()}/api/posts/search?${params}`, {
      cache: 'no-store',
      headers: this.buildHeaders(options),
    });

    if (!response.ok) {
      await this.handleError(response);
    }

    return response.json();
  }

  // ==========================================================================
  // AUTHENTICATED ENDPOINTS
  // ==========================================================================

  /**
   * Get current authenticated user's posts.
   * Requires authentication.
   */
  async getMyPosts(
    page: number = 0,
    size: number = 20,
    options?: RequestOptions
  ): Promise<PageResponse<Post>> {
    if (!options?.accessToken) {
      throw new Error('Authentication required');
    }

    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    const response = await fetch(`${this.getBaseUrl()}/api/posts/me?${params}`, {
      cache: 'no-store',
      headers: this.buildHeaders(options),
    });

    if (!response.ok) {
      await this.handleError(response);
    }

    return response.json();
  }

  /**
   * Create a new post.
   * Requires authentication.
   */
  async create(data: CreatePostRequest, options?: RequestOptions): Promise<Post> {
    if (!options?.accessToken) {
      throw new Error('Authentication required');
    }

    const response = await fetch(`${this.getBaseUrl()}/api/posts`, {
      method: 'POST',
      headers: this.buildHeaders(options, 'application/json'),
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      await this.handleError(response);
    }

    return response.json();
  }

  /**
   * Update an existing post.
   * Requires authentication and ownership (or admin role).
   */
  async update(id: string, data: UpdatePostRequest, options?: RequestOptions): Promise<Post> {
    if (!options?.accessToken) {
      throw new Error('Authentication required');
    }

    const response = await fetch(`${this.getBaseUrl()}/api/posts/${id}`, {
      method: 'PUT',
      headers: this.buildHeaders(options, 'application/json'),
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      await this.handleError(response);
    }

    return response.json();
  }

  /**
   * Delete a post.
   * Requires authentication and ownership (or admin role).
   */
  async delete(id: string, options?: RequestOptions): Promise<void> {
    if (!options?.accessToken) {
      throw new Error('Authentication required');
    }

    const response = await fetch(`${this.getBaseUrl()}/api/posts/${id}`, {
      method: 'DELETE',
      headers: this.buildHeaders(options),
    });

    if (!response.ok) {
      await this.handleError(response);
    }
  }

  /**
   * Toggle like on a post (like if not liked, unlike if liked).
   * Requires authentication.
   */
  async toggleLike(id: string, options?: RequestOptions): Promise<LikeResponse> {
    if (!options?.accessToken) {
      throw new Error('Authentication required');
    }

    const response = await fetch(`${this.getBaseUrl()}/api/posts/${id}/like`, {
      method: 'POST',
      headers: this.buildHeaders(options),
    });

    if (!response.ok) {
      await this.handleError(response);
    }

    return response.json();
  }

  /**
   * Check if a post exists.
   */
  async exists(id: string): Promise<boolean> {
    const response = await fetch(`${this.getBaseUrl()}/api/posts/${id}`, {
      method: 'HEAD',
    });

    return response.ok;
  }
}

// Export singleton instance
export const postApi = new PostApi();
export default postApi;
