import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { addProperty } from '@/lib/mockData';
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
    } = body;

    // Validate required fields
    if (!title || !description || !price || !address || !city ||
        !bedrooms || !bathrooms || !area || !propertyType || !status) {
      return NextResponse.json(
        { error: 'Missing required fields' },
        { status: 400 }
      );
    }

    // Create new property
    const newProperty = addProperty({
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
    });

    return NextResponse.json(
      {
        success: true,
        property: newProperty
      },
      { status: 201 }
    );

  } catch (error) {
    console.error('Error creating property:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
