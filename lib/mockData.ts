import { Property, User, PropertyType, PropertyStatus } from '@/types';

// Mock users for authentication
export const mockUsers: User[] = [
  {
    id: 'user-1',
    name: 'Nguyễn Văn A',
    email: 'user@example.com',
    password: 'password123',
  },
  {
    id: 'user-2',
    name: 'Trần Thị B',
    email: 'admin@example.com',
    password: 'admin123',
  },
];

// Vietnamese cities
const cities = [
  'Hà Nội',
  'TP. Hồ Chí Minh',
  'Đà Nẵng',
  'Hải Phòng',
  'Cần Thơ',
  'Nha Trang',
  'Vũng Tàu',
  'Huế',
  'Biên Hòa',
  'Thủ Đức',
];

// Property types
const propertyTypes: PropertyType[] = ['house', 'apartment', 'villa', 'townhouse'];
const statuses: PropertyStatus[] = ['for-sale', 'for-rent'];

// Vietnamese street names
const streets = [
  'Nguyễn Huệ',
  'Lê Lợi',
  'Trần Phú',
  'Hai Bà Trưng',
  'Lý Thường Kiệt',
  'Hoàng Diệu',
  'Phan Đình Phùng',
  'Võ Thị Sáu',
  'Nguyễn Văn Linh',
  'Lê Duẩn',
];

// Property features
const features = [
  'Bãi đậu xe',
  'Sân vườn',
  'Hồ bơi',
  'Phòng gym',
  'An ninh 24/7',
  'Thang máy',
  'Sân thượng',
  'Ban công',
  'Phòng giặt',
  'Phòng làm việc',
];

// Property name templates
const propertyNames = [
  'Villa',
  'Nhà phố',
  'Căn hộ',
  'Biệt thự',
  'Nhà ở',
  'Chung cư',
  'Penthouse',
  'Duplex',
];

// Generate random number in range
const randomInt = (min: number, max: number): number => {
  return Math.floor(Math.random() * (max - min + 1)) + min;
};

// Generate random array elements
const randomElements = <T>(arr: T[], count: number): T[] => {
  const shuffled = [...arr].sort(() => 0.5 - Math.random());
  return shuffled.slice(0, count);
};

// Generate mock properties
export const generateMockProperties = (count: number = 100): Property[] => {
  const properties: Property[] = [];

  for (let i = 1; i <= count; i++) {
    const propertyType = propertyTypes[randomInt(0, propertyTypes.length - 1)];
    const city = cities[randomInt(0, cities.length - 1)];
    const street = streets[randomInt(0, streets.length - 1)];
    const status = statuses[randomInt(0, statuses.length - 1)];
    const propertyName = propertyNames[randomInt(0, propertyNames.length - 1)];

    // Generate images array (5 images per property)
    const images = Array.from(
      { length: 5 },
      (_, idx) => `/images/properties/property-${i}-${idx + 1}.jpg`
    );

    // Generate property features (2-5 random features)
    const propertyFeatures = randomElements(features, randomInt(2, 5));

    // Calculate price based on property type and status
    let basePrice = 0;
    switch (propertyType) {
      case 'villa':
        basePrice = randomInt(8000000000, 50000000000); // 8-50 billion VND
        break;
      case 'townhouse':
        basePrice = randomInt(3000000000, 15000000000); // 3-15 billion VND
        break;
      case 'house':
        basePrice = randomInt(2000000000, 10000000000); // 2-10 billion VND
        break;
      case 'apartment':
        basePrice = randomInt(1000000000, 5000000000); // 1-5 billion VND
        break;
    }

    // If for rent, divide by 200 to get monthly rent
    const price = status === 'for-rent' ? Math.floor(basePrice / 200) : basePrice;

    properties.push({
      id: `prop-${i}`,
      title: `${propertyName} ${i} tại ${city}`,
      description: `${propertyName} hiện đại, thiết kế sang trọng, nội thất cao cấp. Vị trí đắc địa, gần trung tâm thành phố, tiện ích đầy đủ. Không gian sống thoáng mát, an ninh tốt, phù hợp cho gia đình hoặc đầu tư.`,
      price,
      address: `${randomInt(1, 999)} ${street}, ${city}`,
      city,
      bedrooms: randomInt(1, 6),
      bathrooms: randomInt(1, 5),
      area: randomInt(50, 500),
      propertyType,
      status,
      images,
      createdAt: new Date(
        Date.now() - randomInt(0, 365) * 24 * 60 * 60 * 1000
      ).toISOString(),
      userId: mockUsers[randomInt(0, mockUsers.length - 1)].id,
      features: propertyFeatures,
    });
  }

  // Sort by createdAt (newest first)
  return properties.sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
};

// Export generated properties - using let to allow mutations
let mockPropertiesData = generateMockProperties(100);

// Export getter to always return current data
export const getMockProperties = (): Property[] => {
  return mockPropertiesData;
};

// For backward compatibility
export const mockProperties = mockPropertiesData;

// Function to add a new property
export const addProperty = (property: Omit<Property, 'id' | 'createdAt' | 'images'>): Property => {
  // Generate new ID
  const existingIds = mockPropertiesData.map(p => {
    const num = parseInt(p.id.replace('prop-', ''));
    return isNaN(num) ? 0 : num;
  });
  const maxId = Math.max(...existingIds, 100);
  const newId = `prop-${maxId + 1}`;

  // Use existing property images (cycling through available ones)
  const imageIndex = ((maxId % 100) || 100);
  const images = Array.from(
    { length: 5 },
    (_, idx) => `/images/properties/property-${imageIndex}-${idx + 1}.jpg`
  );

  const newProperty: Property = {
    ...property,
    id: newId,
    images,
    createdAt: new Date().toISOString(),
  };

  // Add to beginning of array (newest first)
  mockPropertiesData.unshift(newProperty);

  return newProperty;
};
