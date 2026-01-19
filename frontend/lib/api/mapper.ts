import { Property as ApiProperty, PropertyType as ApiPropertyType, PropertyStatus as ApiPropertyStatus } from '@/types/api';
import { Property as UiProperty, PropertyType as UiPropertyType, PropertyStatus as UiPropertyStatus } from '@/types';

/**
 * Mapper functions to convert between API types and UI types
 */

/**
 * Convert API PropertyType to UI PropertyType
 */
export function mapApiPropertyTypeToUi(apiType: ApiPropertyType): UiPropertyType {
  const mapping: Record<ApiPropertyType, UiPropertyType> = {
    'HOUSE': 'house',
    'APARTMENT': 'apartment',
    'VILLA': 'villa',
    'TOWNHOUSE': 'townhouse',
  };
  return mapping[apiType];
}

/**
 * Convert UI PropertyType to API PropertyType
 */
export function mapUiPropertyTypeToApi(uiType: UiPropertyType): ApiPropertyType {
  const mapping: Record<UiPropertyType, ApiPropertyType> = {
    'house': 'HOUSE',
    'apartment': 'APARTMENT',
    'villa': 'VILLA',
    'townhouse': 'TOWNHOUSE',
  };
  return mapping[uiType];
}

/**
 * Convert API PropertyStatus to UI PropertyStatus
 */
export function mapApiPropertyStatusToUi(apiStatus: ApiPropertyStatus): UiPropertyStatus {
  const mapping: Record<ApiPropertyStatus, UiPropertyStatus> = {
    'FOR_SALE': 'for-sale',
    'FOR_RENT': 'for-rent',
  };
  return mapping[apiStatus];
}

/**
 * Convert UI PropertyStatus to API PropertyStatus
 */
export function mapUiPropertyStatusToApi(uiStatus: UiPropertyStatus): ApiPropertyStatus {
  const mapping: Record<UiPropertyStatus, ApiPropertyStatus> = {
    'for-sale': 'FOR_SALE',
    'for-rent': 'FOR_RENT',
  };
  return mapping[uiStatus];
}

/**
 * Convert API Property to UI Property
 */
export function mapApiPropertyToUi(apiProperty: ApiProperty): UiProperty {
  return {
    id: apiProperty.id.toString(),
    title: apiProperty.title,
    description: apiProperty.description,
    price: apiProperty.price,
    address: apiProperty.address,
    city: apiProperty.city,
    bedrooms: apiProperty.bedrooms,
    bathrooms: apiProperty.bathrooms,
    area: apiProperty.area,
    propertyType: mapApiPropertyTypeToUi(apiProperty.propertyType),
    status: mapApiPropertyStatusToUi(apiProperty.status),
    images: apiProperty.images,
    features: apiProperty.features,
    userId: apiProperty.userId,
    createdAt: apiProperty.createdAt,
  };
}

/**
 * Convert UI Property to API Create Property Request
 */
export function mapUiPropertyToApiCreate(uiProperty: Omit<UiProperty, 'id' | 'createdAt'>) {
  return {
    title: uiProperty.title,
    description: uiProperty.description,
    price: uiProperty.price,
    address: uiProperty.address,
    city: uiProperty.city,
    bedrooms: uiProperty.bedrooms,
    bathrooms: uiProperty.bathrooms,
    area: uiProperty.area,
    propertyType: mapUiPropertyTypeToApi(uiProperty.propertyType),
    status: mapUiPropertyStatusToApi(uiProperty.status),
    images: uiProperty.images,
    features: uiProperty.features,
    userId: uiProperty.userId,
  };
}
