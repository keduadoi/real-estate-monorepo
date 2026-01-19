'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { PropertyType, PropertyStatus } from '@/types';

export default function PropertyForm() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    price: '',
    address: '',
    city: '',
    bedrooms: '1',
    bathrooms: '1',
    area: '',
    propertyType: 'house' as PropertyType,
    status: 'for-sale' as PropertyStatus,
  });

  const [selectedFeatures, setSelectedFeatures] = useState<string[]>([]);

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

  const availableFeatures = [
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

  const handleFeatureToggle = (feature: string) => {
    setSelectedFeatures((prev) =>
      prev.includes(feature)
        ? prev.filter((f) => f !== feature)
        : [...prev, feature]
    );
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // Validate form
      if (!formData.title || !formData.description || !formData.price ||
          !formData.address || !formData.city || !formData.area) {
        setError('Vui lòng điền đầy đủ thông tin bắt buộc');
        setLoading(false);
        return;
      }

      // Submit to API
      const response = await fetch('/api/properties', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          ...formData,
          features: selectedFeatures,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setError(data.error || 'Đã có lỗi xảy ra. Vui lòng thử lại.');
        setLoading(false);
        return;
      }

      // Success - redirect to home page
      router.push('/');
      router.refresh();
    } catch (err) {
      setError('Đã có lỗi xảy ra. Vui lòng thử lại.');
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div className="rounded-md bg-red-50 p-4">
          <p className="text-sm text-red-800">{error}</p>
        </div>
      )}

      <div>
        <label htmlFor="title" className="block text-sm font-medium text-gray-700">
          Tiêu đề <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          id="title"
          required
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          placeholder="VD: Villa 2 tầng tại Hà Nội"
        />
      </div>

      <div>
        <label htmlFor="description" className="block text-sm font-medium text-gray-700">
          Mô tả <span className="text-red-500">*</span>
        </label>
        <textarea
          id="description"
          required
          rows={4}
          value={formData.description}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          placeholder="Mô tả chi tiết về bất động sản..."
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label htmlFor="propertyType" className="block text-sm font-medium text-gray-700">
            Loại hình <span className="text-red-500">*</span>
          </label>
          <select
            id="propertyType"
            required
            value={formData.propertyType}
            onChange={(e) =>
              setFormData({ ...formData, propertyType: e.target.value as PropertyType })
            }
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="house">Nhà</option>
            <option value="apartment">Căn hộ</option>
            <option value="villa">Biệt thự</option>
            <option value="townhouse">Nhà phố</option>
          </select>
        </div>

        <div>
          <label htmlFor="status" className="block text-sm font-medium text-gray-700">
            Trạng thái <span className="text-red-500">*</span>
          </label>
          <select
            id="status"
            required
            value={formData.status}
            onChange={(e) =>
              setFormData({ ...formData, status: e.target.value as PropertyStatus })
            }
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="for-sale">Cần bán</option>
            <option value="for-rent">Cho thuê</option>
          </select>
        </div>
      </div>

      <div>
        <label htmlFor="price" className="block text-sm font-medium text-gray-700">
          Giá (VND) <span className="text-red-500">*</span>
        </label>
        <input
          type="number"
          id="price"
          required
          min="0"
          value={formData.price}
          onChange={(e) => setFormData({ ...formData, price: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          placeholder="5000000000"
        />
      </div>

      <div>
        <label htmlFor="address" className="block text-sm font-medium text-gray-700">
          Địa chỉ <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          id="address"
          required
          value={formData.address}
          onChange={(e) => setFormData({ ...formData, address: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          placeholder="123 Nguyễn Huệ"
        />
      </div>

      <div>
        <label htmlFor="city" className="block text-sm font-medium text-gray-700">
          Thành phố <span className="text-red-500">*</span>
        </label>
        <select
          id="city"
          required
          value={formData.city}
          onChange={(e) => setFormData({ ...formData, city: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
        >
          <option value="">Chọn thành phố</option>
          {cities.map((city) => (
            <option key={city} value={city}>
              {city}
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div>
          <label htmlFor="bedrooms" className="block text-sm font-medium text-gray-700">
            Số phòng ngủ <span className="text-red-500">*</span>
          </label>
          <input
            type="number"
            id="bedrooms"
            required
            min="1"
            value={formData.bedrooms}
            onChange={(e) => setFormData({ ...formData, bedrooms: e.target.value })}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          />
        </div>

        <div>
          <label htmlFor="bathrooms" className="block text-sm font-medium text-gray-700">
            Số phòng tắm <span className="text-red-500">*</span>
          </label>
          <input
            type="number"
            id="bathrooms"
            required
            min="1"
            value={formData.bathrooms}
            onChange={(e) => setFormData({ ...formData, bathrooms: e.target.value })}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          />
        </div>

        <div>
          <label htmlFor="area" className="block text-sm font-medium text-gray-700">
            Diện tích (m²) <span className="text-red-500">*</span>
          </label>
          <input
            type="number"
            id="area"
            required
            min="1"
            value={formData.area}
            onChange={(e) => setFormData({ ...formData, area: e.target.value })}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Tiện ích
        </label>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
          {availableFeatures.map((feature) => (
            <label key={feature} className="flex items-center">
              <input
                type="checkbox"
                checked={selectedFeatures.includes(feature)}
                onChange={() => handleFeatureToggle(feature)}
                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
              />
              <span className="ml-2 text-sm text-gray-700">{feature}</span>
            </label>
          ))}
        </div>
      </div>

      <div className="bg-blue-50 p-4 rounded-md">
        <p className="text-sm text-blue-800">
          Lưu ý: Hình ảnh sẽ được tự động sử dụng từ thư viện mẫu. Trong phiên bản
          thực tế, bạn sẽ có thể tải lên hình ảnh của riêng mình.
        </p>
      </div>

      <div className="flex gap-4">
        <button
          type="submit"
          disabled={loading}
          className="flex-1 bg-primary-600 text-white py-2 px-4 rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? 'Đang đăng tin...' : 'Đăng tin'}
        </button>
        <button
          type="button"
          onClick={() => router.back()}
          className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
        >
          Hủy
        </button>
      </div>
    </form>
  );
}
