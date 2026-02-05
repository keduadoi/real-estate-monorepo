import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { writeFile, mkdir } from 'fs/promises';
import { join } from 'path';
import { existsSync } from 'fs';

// Use Kong Gateway for backend access (backend runs in K8s)
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8000';

// Store mock uploads in memory with their file paths
declare global {
  var mockUploads: Map<string, string>;
}

if (!global.mockUploads) {
  global.mockUploads = new Map();
}

async function tryBackendUpload(
  formData: FormData,
  accessToken?: string,
  user?: { id?: string; email?: string | null; name?: string | null }
): Promise<Response | null> {
  try {
    const headers: HeadersInit = {};
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }
    if (user?.id) {
      headers['X-User-Id'] = user.id;
    }
    if (user?.email) {
      headers['X-User-Email'] = user.email;
    }
    if (user?.name) {
      headers['X-User-Name'] = user.name;
    }

    const response = await fetch(`${BACKEND_URL}/api/upload/temp`, {
      method: 'POST',
      headers,
      body: formData,
    });

    return response;
  } catch (error) {
    console.log('Backend not available for upload, using local storage');
    return null;
  }
}

export async function POST(request: NextRequest) {
  const session = await getServerSession(authOptions);

  if (!session?.user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const formData = await request.formData();
    const files = formData.getAll('files') as File[];

    if (!files || files.length === 0) {
      return NextResponse.json({ error: 'No files provided' }, { status: 400 });
    }

    const accessToken = session.accessToken;

    // Try backend first
    const backendFormData = new FormData();
    for (const file of files) {
      backendFormData.append('files', file);
    }

    const backendResponse = await tryBackendUpload(backendFormData, accessToken, session.user);

    if (backendResponse?.ok) {
      const data = await backendResponse.json();
      return NextResponse.json(data);
    }

    // If backend returned an error (401, 403, 500, etc.) or is unavailable,
    // fall back to local storage instead of forwarding the error
    if (backendResponse && !backendResponse.ok) {
      console.log(`Backend upload returned ${backendResponse.status}, falling back to local storage`);
    }

    // Fallback: save files locally in public/uploads/temp
    const uploadDir = join(process.cwd(), 'public', 'uploads', 'temp');

    if (!existsSync(uploadDir)) {
      await mkdir(uploadDir, { recursive: true });
    }

    const imageUrls: string[] = [];

    for (const file of files) {
      const bytes = await file.arrayBuffer();
      const buffer = Buffer.from(bytes);

      // Generate unique filename
      const uniqueId = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
      const extension = file.name.split('.').pop() || 'jpg';
      const filename = `${uniqueId}.${extension}`;
      const filepath = join(uploadDir, filename);

      await writeFile(filepath, buffer);

      // Return URL that points to the local file
      const imageUrl = `/uploads/temp/${filename}`;
      imageUrls.push(imageUrl);

      // Store in memory for tracking
      global.mockUploads.set(imageUrl, filepath);
    }

    return NextResponse.json({
      imageUrls,
      totalUploaded: imageUrls.length,
    });
  } catch (error) {
    console.error('Upload error:', error);
    return NextResponse.json(
      { error: 'Failed to upload images' },
      { status: 500 }
    );
  }
}
