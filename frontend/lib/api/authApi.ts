const AUTH_API_URL = process.env.NEXT_PUBLIC_AUTH_API_URL || 'http://localhost:8081';

export interface RegisterRequest {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  user: {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
    enabled: boolean;
    roles: string[];
    createdAt: string;
    lastLoginAt: string;
  };
}

class AuthApi {
  async register(request: RegisterRequest): Promise<AuthResponse> {
    const response = await fetch(`${AUTH_API_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => null);
      throw new Error(error?.message || 'Đăng ký thất bại');
    }

    return response.json();
  }
}

export const authApi = new AuthApi();
