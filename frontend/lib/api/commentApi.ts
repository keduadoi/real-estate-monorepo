import {
  CaptchaChallenge,
  CommentDTO,
  CommentListResponse,
  CreateCommentRequest,
  ReportCommentRequest,
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

class CommentApi {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  private buildHeaders(opts?: RequestOptions, contentType?: string): HeadersInit {
    const h: HeadersInit = {};
    if (contentType) h['Content-Type'] = contentType;
    if (opts?.accessToken) h['Authorization'] = `Bearer ${opts.accessToken}`;
    if (opts?.user) {
      h['X-User-Id'] = opts.user.id;
      if (opts.user.email) h['X-User-Email'] = opts.user.email;
      if (opts.user.name) h['X-User-Name'] = opts.user.name;
      if (opts.user.roles?.length) h['X-User-Roles'] = opts.user.roles.join(',');
    }
    return h;
  }

  async list(propertyId: number, page = 0, perPage = 10, opts?: RequestOptions): Promise<CommentListResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    const res = await fetch(
      `${this.baseUrl}/api/properties/${propertyId}/comments?${params.toString()}`,
      { headers: this.buildHeaders(opts), cache: 'no-store' },
    );
    if (!res.ok) throw new Error(`Failed to load comments: ${res.statusText}`);
    return res.json();
  }

  async getCaptcha(): Promise<CaptchaChallenge> {
    const res = await fetch(`${this.baseUrl}/api/captcha`, { cache: 'no-store' });
    if (!res.ok) throw new Error(`Failed to fetch captcha: ${res.statusText}`);
    return res.json();
  }

  async create(propertyId: number, body: CreateCommentRequest, opts?: RequestOptions): Promise<CommentDTO> {
    const res = await fetch(`${this.baseUrl}/api/properties/${propertyId}/comments`, {
      method: 'POST',
      headers: this.buildHeaders(opts, 'application/json'),
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: res.statusText }));
      throw new Error(err.message || `Failed to post comment (${res.status})`);
    }
    return res.json();
  }

  async reply(parentId: number, body: CreateCommentRequest, opts?: RequestOptions): Promise<CommentDTO> {
    const res = await fetch(`${this.baseUrl}/api/comments/${parentId}/reply`, {
      method: 'POST',
      headers: this.buildHeaders(opts, 'application/json'),
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: res.statusText }));
      throw new Error(err.message || `Failed to post reply (${res.status})`);
    }
    return res.json();
  }

  async update(id: number, body: string, opts?: RequestOptions): Promise<CommentDTO> {
    const res = await fetch(`${this.baseUrl}/api/comments/${id}`, {
      method: 'PUT',
      headers: this.buildHeaders(opts, 'application/json'),
      body: JSON.stringify({ body }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: res.statusText }));
      throw new Error(err.message || `Failed to update comment (${res.status})`);
    }
    return res.json();
  }

  async remove(id: number, opts?: RequestOptions): Promise<void> {
    const res = await fetch(`${this.baseUrl}/api/comments/${id}`, {
      method: 'DELETE',
      headers: this.buildHeaders(opts),
    });
    if (!res.ok && res.status !== 204) {
      const err = await res.json().catch(() => ({ message: res.statusText }));
      throw new Error(err.message || `Failed to delete comment (${res.status})`);
    }
  }

  async like(id: number, opts?: RequestOptions): Promise<{ liked: boolean }> {
    const res = await fetch(`${this.baseUrl}/api/comments/${id}/like`, {
      method: 'POST',
      headers: this.buildHeaders(opts, 'application/json'),
    });
    if (!res.ok) throw new Error(`Failed to like comment: ${res.statusText}`);
    return res.json();
  }

  async report(id: number, body: ReportCommentRequest, opts?: RequestOptions): Promise<void> {
    const res = await fetch(`${this.baseUrl}/api/comments/${id}/report`, {
      method: 'POST',
      headers: this.buildHeaders(opts, 'application/json'),
      body: JSON.stringify(body),
    });
    if (!res.ok && res.status !== 202) {
      throw new Error(`Failed to report comment: ${res.statusText}`);
    }
  }

  // ---- Admin --------------------------------------------------------------

  async listReported(page = 0, perPage = 20, opts?: RequestOptions): Promise<CommentListResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    const res = await fetch(`${this.baseUrl}/api/admin/comments?${params.toString()}`, {
      headers: this.buildHeaders(opts),
      cache: 'no-store',
    });
    if (!res.ok) throw new Error(`Failed to load reports: ${res.statusText}`);
    return res.json();
  }

  async hide(id: number, reason: string, opts?: RequestOptions): Promise<void> {
    const res = await fetch(`${this.baseUrl}/api/admin/comments/${id}/hide`, {
      method: 'POST',
      headers: this.buildHeaders(opts, 'application/json'),
      body: JSON.stringify({ reason }),
    });
    if (!res.ok && res.status !== 204) throw new Error(`Failed to hide comment: ${res.statusText}`);
  }

  async restore(id: number, opts?: RequestOptions): Promise<void> {
    const res = await fetch(`${this.baseUrl}/api/admin/comments/${id}/restore`, {
      method: 'POST',
      headers: this.buildHeaders(opts),
    });
    if (!res.ok && res.status !== 204) throw new Error(`Failed to restore comment: ${res.statusText}`);
  }
}

export const commentApi = new CommentApi();
export default commentApi;
