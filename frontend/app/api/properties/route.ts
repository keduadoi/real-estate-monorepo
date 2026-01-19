import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { propertyApi } from '@/lib/api/propertyApi';
import { mapApiPropertyToUi, mapUiPropertyToApiCreate } from '@/lib/api/mapper';
import { PropertyType, PropertyStatus } from '@/types';

export async function POST(request: NextRequest) {
  try {
    // Check authentication
    const session = await getServerSession(authOptions);

    if (!session || !session.user) {
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      );
    }

    // Parse request body
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

    // Create API request from UI data
    const apiRequest = mapUiPropertyToApiCreate({
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
      userId: session.user.id,
      features: features || [],
      images: images || [],
    });

    // Create property via backend API
    const apiProperty = await propertyApi.create(apiRequest);
    const uiProperty = mapApiPropertyToUi(apiProperty);

    return NextResponse.json(
      {
        success: true,
        property: uiProperty
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
