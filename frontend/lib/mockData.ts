import { Property, User, PropertyType, PropertyStatus, Post, Like, PostWithMetadata } from '@/types';

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

// ============================================
// POSTS & LIKES MOCK DATA
// ============================================

// Generate initial mock posts
const generateMockPosts = (): Post[] => {
  const postsContent = [
    'Chào mọi người! Mình mới tham gia cộng đồng này. Rất vui được gặp các bạn yêu thích bất động sản như mình.',
    'Có ai đang tìm căn hộ 2 phòng ngủ ở Hà Nội không? Mình đang có một căn view đẹp, giá hợp lý. Inbox để xem thêm nhé!',
    'Vừa xem một villa tuyệt đẹp ở Đà Nẵng. Thiết kế hiện đại, view biển cực kỳ ấn tượng. Ai quan tâm không ạ?',
    'Thị trường bất động sản năm nay có xu hướng thế nào? Mọi người nghĩ sao về việc đầu tư vào căn hộ mini?',
    'Chia sẻ kinh nghiệm: Khi mua nhà, nên chú ý đến vị trí và pháp lý trước tiên. Giá cả có thể thương lượng nhưng 2 yếu tố này không thể thay đổi.',
    'Khu vực Thủ Đức đang phát triển rất nhanh. Có ai đang quan tâm đến bất động sản ở đây không?',
    'Nhà phố 3 tầng tại Hải Phòng, giá tốt, gần trung tâm. Phù hợp cho gia đình hoặc kinh doanh.',
    'Mình đang phân vân giữa mua chung cư cao cấp và biệt thự vùng ven. Các bạn có lời khuyên gì không?',
    'Vừa hoàn thành thủ tục mua căn hộ đầu tiên! Cảm giác tuyệt vời khi có được tổ ấm của riêng mình.',
    'Những lưu ý khi thuê nhà: Kiểm tra hợp đồng kỹ, xem xét điều khoản thanh lý, và chụp ảnh hiện trạng nhà cửa trước khi nhận.',
    'Giá bất động sản tại TP.HCM có xu hướng tăng nhẹ. Đây có phải thời điểm tốt để đầu tư không?',
    'Mình có một lô đất ở Nha Trang, diện tích 200m2, gần biển. Ai quan tâm liên hệ nhé!',
    'Kinh nghiệm vay ngân hàng mua nhà: Chuẩn bị hồ sơ kỹ, so sánh lãi suất nhiều ngân hàng trước khi quyết định.',
    'Căn hộ studio có phù hợp với người độc thân không? Mọi người cho ý kiến với.',
    'Vừa tham quan dự án mới ở Biên Hòa, giá khá hấp dẫn. Khu vực đang phát triển mạnh.',
    'Ai có kinh nghiệm đầu tư căn hộ cho thuê? Tỷ suất lợi nhuận thế nào?',
    'Thủ tục chuyển nhượng nhà đất cần những giấy tờ gì? Mình đang cần tư vấn.',
    'Penthouse view sông tại quận 2, thiết kế sang trọng. Giá 8 tỷ. Xem ảnh inbox nhé!',
    'Nên mua nhà cũ sửa lại hay mua nhà mới? Mọi người thảo luận cùng mình.',
    'Khu đô thị mới ở Hà Nội đang rất sôi động. Nhiều dự án chất lượng cao.',
    'Mình vừa bán được căn hộ sau 3 tháng rao. Chia sẻ kinh nghiệm: Chụp ảnh đẹp, giá hợp lý, và kiên nhẫn!',
    'Có nên đầu tư vào shophouse không? Rủi ro và lợi nhuận như thế nào?',
    'Tìm kiếm căn hộ 3PN tại Cầu Giấy, Hà Nội. Ngân sách 4-5 tỷ. Ai có thông tin giúp mình với!',
    'Vùng ven Hà Nội đang rất tiềm năng. Giá còn phải chăng, cơ sở hạ tầng đang phát triển.',
    'Lưu ý quan trọng: Luôn kiểm tra pháp lý kỹ trước khi mua. Sổ đỏ, giấy phép xây dựng phải đầy đủ.',
    'Mọi người nghĩ sao về xu hướng căn hộ thông minh? Có đáng để đầu tư thêm không?',
    'Vừa xem một dự án resort tại Phú Quốc. Vị trí đẹp nhưng giá cao. Cân nhắc nên mua hay không.',
    'Ai đã từng đầu tư condotel? Chia sẻ kinh nghiệm cho mọi người với.',
    'Thị trường bất động sản hiện tại: Nên giữ tiền mặt hay đầu tư ngay? Mọi người nghĩ sao?',
    'Townhouse 4 tầng tại Thanh Xuân, Hà Nội. Vị trí đẹp, gần trường học. Giá 12 tỷ có thương lượng.',
  ];

  const posts: Post[] = [];
  const now = Date.now();

  for (let i = 0; i < 30; i++) {
    posts.push({
      id: `post-${i + 1}`,
      content: postsContent[i],
      userId: mockUsers[i % 2].id, // Alternate between user-1 and user-2
      createdAt: new Date(now - randomInt(0, 30) * 24 * 60 * 60 * 1000).toISOString(), // Random time in last 30 days
    });
  }

  return posts.sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
};

// Generate initial mock likes
const generateMockLikes = (posts: Post[]): Like[] => {
  const likes: Like[] = [];
  let likeId = 1;

  posts.forEach((post) => {
    // Random number of likes (0-15) for each post
    const likeCount = randomInt(0, 15);
    const now = Date.now();

    for (let i = 0; i < likeCount; i++) {
      likes.push({
        id: `like-${likeId++}`,
        postId: post.id,
        userId: mockUsers[i % 2].id, // Alternate between users
        createdAt: new Date(now - randomInt(0, 7) * 24 * 60 * 60 * 1000).toISOString(),
      });
    }
  });

  return likes;
};

// Initialize mock data
let mockPostsData: Post[] = generateMockPosts();
let mockLikesData: Like[] = generateMockLikes(mockPostsData);

// Get all posts
export const getPosts = (): Post[] => {
  return mockPostsData;
};

// Get post by ID
export const getPostById = (id: string): Post | undefined => {
  return mockPostsData.find(post => post.id === id);
};

// Add a new post
export const addPost = (post: Omit<Post, 'id' | 'createdAt'>): Post => {
  // Generate new ID
  const existingIds = mockPostsData.map(p => {
    const num = parseInt(p.id.replace('post-', ''));
    return isNaN(num) ? 0 : num;
  });
  const maxId = Math.max(...existingIds, 0);
  const newId = `post-${maxId + 1}`;

  const newPost: Post = {
    ...post,
    id: newId,
    createdAt: new Date().toISOString(),
  };

  // Add to beginning of array (newest first)
  mockPostsData.unshift(newPost);

  return newPost;
};

// Get likes for a specific post
export const getLikesForPost = (postId: string): Like[] => {
  return mockLikesData.filter(like => like.postId === postId);
};

// Add a like
export const addLike = (postId: string, userId: string): Like => {
  // Check if user already liked this post
  const existingLike = mockLikesData.find(
    like => like.postId === postId && like.userId === userId
  );

  if (existingLike) {
    throw new Error('User already liked this post');
  }

  // Generate new like ID
  const existingIds = mockLikesData.map(l => {
    const num = parseInt(l.id.replace('like-', ''));
    return isNaN(num) ? 0 : num;
  });
  const maxId = Math.max(...existingIds, 0);
  const newId = `like-${maxId + 1}`;

  const newLike: Like = {
    id: newId,
    postId,
    userId,
    createdAt: new Date().toISOString(),
  };

  mockLikesData.push(newLike);

  return newLike;
};

// Remove a like
export const removeLike = (postId: string, userId: string): void => {
  mockLikesData = mockLikesData.filter(
    like => !(like.postId === postId && like.userId === userId)
  );
};

// Get posts with metadata (likes, user info)
export const getPostsWithMetadata = (currentUserId?: string): PostWithMetadata[] => {
  return mockPostsData.map(post => {
    // Find the user who created the post
    const user = mockUsers.find(u => u.id === post.userId);

    // Get likes for this post
    const postLikes = getLikesForPost(post.id);

    // Check if current user liked this post
    const isLikedByCurrentUser = currentUserId
      ? postLikes.some(like => like.userId === currentUserId)
      : false;

    return {
      ...post,
      user: user ? {
        id: user.id,
        name: user.name,
        email: user.email,
      } : {
        id: 'unknown',
        name: 'Unknown User',
        email: 'unknown@example.com',
      },
      likeCount: postLikes.length,
      isLikedByCurrentUser,
      likes: postLikes,
    };
  }).sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
};
