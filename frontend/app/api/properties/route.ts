import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { PropertyType, PropertyStatus } from '@/types';

// Use Kong Gateway for backend access (backend runs in K8s)
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8000';

// In-memory store for mock properties
declare global {
  var mockProperties: any[];
  var propertyIdCounter: number;
}

if (!global.mockProperties) {
  global.mockProperties = [];
}
if (!global.propertyIdCounter) {
  global.propertyIdCounter = 1;
}

async function tryBackendRequest(
  method: string,
  path: string,
  body?: any,
  accessToken?: string,
  user?: { id?: string; email?: string | null; name?: string | null }
): Promise<Response | null> {
  try {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };
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

    const response = await fetch(`${BACKEND_URL}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    return response;
  } catch (error) {
    console.log('Backend not available for properties, using mock data');
    return null;
  }
}

export async function POST(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);

    if (!session || !session.user) {
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    const body = await request.json();
    const {
      title,
      description,
      price,
      address,
      city,
      bedrooms,
      bathrooms,
      area,
      propertyType,
      status,
      features,
      images,
    } = body;

    // Validate required fields
    if (!title || !description || !price || !address || !city ||
        !bedrooms || !bathrooms || !area || !propertyType || !status) {
      return NextResponse.json(
        { error: 'Missing required fields' },
        { status: 400 }
      );
    }

    const accessToken = session.accessToken;

    // Convert frontend values to backend enum format
    const propertyTypeMap: Record<string, string> = {
      'house': 'HOUSE',
      'apartment': 'APARTMENT',
      'villa': 'VILLA',
      'townhouse': 'TOWNHOUSE',
    };
    const statusMap: Record<string, string> = {
      'for-sale': 'FOR_SALE',
      'for-rent': 'FOR_RENT',
    };

    // Build API request
    const apiRequest = {
      title,
      description,
      price: Number(price),
      address,
      city,
      bedrooms: Number(bedrooms),
      bathrooms: Number(bathrooms),
      area: Number(area),
      propertyType: propertyTypeMap[propertyType] || propertyType.toUpperCase(),
      status: statusMap[status] || status.toUpperCase().replace('-', '_'),
      features: features || [],
      images: images || [],
    };

    // Try backend first
    const backendResponse = await tryBackendRequest('POST', '/api/properties', apiRequest, accessToken, session.user);

    if (backendResponse?.ok) {
      const data = await backendResponse.json();
      return NextResponse.json({ success: true, property: data }, { status: 201 });
    }

    // If backend returned error, log and fall back to mock
    if (backendResponse && !backendResponse.ok) {
      const errorBody = await backendResponse.text();
      console.log(`Backend property create returned ${backendResponse.status}: ${errorBody}, falling back to mock`);
    }

    // Fallback to mock data
    const newProperty = {
      id: global.propertyIdCounter++,
      title,
      description,
      price: Number(price),
      address,
      city,
      bedrooms: Number(bedrooms),
      bathrooms: Number(bathrooms),
      area: Number(area),
      propertyType: propertyType as PropertyType,
      status: status as PropertyStatus,
      features: features || [],
      images: images || [],
      userId: session.user.id,
      userName: session.user.name || session.user.email,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    global.mockProperties.unshift(newProperty);

    return NextResponse.json(
      {
        success: true,
        property: newProperty
      },
      { status: 201 }
    );

  } catch (error) {
    console.error('Error creating property:', error);
    const errorMessage = error instanceof Error ? error.message : 'Internal server error';
    return NextResponse.json(
      { error: errorMessage },
      { status: 500 }
    );
  }
}

export async function GET(request: NextRequest) {
  const session = await getServerSession(authOptions);
  const accessToken = session?.accessToken;

  const { searchParams } = new URL(request.url);
  const page = searchParams.get('page') || '0';
  const size = searchParams.get('size') || '20';

  // Try backend first
  const backendResponse = await tryBackendRequest(
    'GET',
    `/api/properties?page=${page}&size=${size}`,
    undefined,
    accessToken,
    session?.user
  );

  if (backendResponse?.ok) {
    const data = await backendResponse.json();
    return NextResponse.json(data);
  }

  // Fallback to mock data
  const start = Number(page) * Number(size);
  const end = start + Number(size);
  const paginatedProperties = global.mockProperties.slice(start, end);

  return NextResponse.json({
    content: paginatedProperties,
    totalElements: global.mockProperties.length,
    totalPages: Math.ceil(global.mockProperties.length / Number(size)),
    number: Number(page),
    size: Number(size),
  });
}
