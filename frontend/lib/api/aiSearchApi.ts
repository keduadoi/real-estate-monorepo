import { AiSearchParseResponse } from '@/types/api';

const API_BASE_URL = process.env.NEXT_PUBLIC_KONG_URL || 'http://127.0.0.1:8000';

class AiSearchApi {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  async parse(query: string, locale = 'vi-VN'): Promise<AiSearchParseResponse> {
    const response = await fetch(`${this.baseUrl}/api/ai-search/parse`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, locale }),
      cache: 'no-store',
    });

    if (!response.ok) {
      const err = await response.json().catch(() => ({ message: response.statusText }));
      throw new Error(err.message || `AI search failed: ${response.statusText}`);
    }
    return response.json();
  }
}

export const aiSearchApi = new AiSearchApi();
export default aiSearchApi;
