'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { useSession } from 'next-auth/react';
import { useTranslations } from 'next-intl';
import { PropertyType, PropertyStatus, Property } from '@/types';
import ImageUpload from './ImageUpload';
import { useImageUpload } from '@/hooks/useImageUpload';
import { propertyApi } from '@/lib/api/propertyApi';
import { mapUiPropertyTypeToApi, mapUiPropertyStatusToApi } from '@/lib/api/mapper';

interface PropertyFormProps {
  mode?: 'create' | 'edit';
  initialData?: Property;
  propertyId?: string;
}

const FEATURE_KEYS = [
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

export default function PropertyForm({
  mode = 'create',
  initialData,
  propertyId,
}: PropertyFormProps) {
  const router = useRouter();
  const { data: session } = useSession();
  const t = useTranslations();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [formData, setFormData] = useState({
    title: initialData?.title || '',
    description: initialData?.description || '',
    price: initialData?.price?.toString() || '',
    address: initialData?.address || '',
    city: initialData?.city || '',
    bedrooms: initialData?.bedrooms?.toString() || '1',
    bathrooms: initialData?.bathrooms?.toString() || '1',
    area: initialData?.area?.toString() || '',
    propertyType: (initialData?.propertyType || 'house') as PropertyType,
    status: (initialData?.status || 'for-sale') as PropertyStatus,
  });

  const [selectedFeatures, setSelectedFeatures] = useState<string[]>(
    initialData?.features || []
  );

  const [existingImages, setExistingImages] = useState<string[]>(
    initialData?.images || []
  );

  const imageUpload = useImageUpload();

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

  const handleFeatureToggle = (feature: string) => {
    setSelectedFeatures((prev) =>
      prev.includes(feature)
        ? prev.filter((f) => f !== feature)
        : [...prev, feature]
    );
  };

  const handleRemoveExistingImage = (imageUrl: string) => {
    setExistingImages((prev) => prev.filter((url) => url !== imageUrl));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (!formData.title || !formData.description || !formData.price ||
          !formData.address || !formData.city || !formData.area) {
        setError(t('properties.form.errors.required'));
        setLoading(false);
        return;
      }

      if (formData.title.length < 10 || formData.title.length > 255) {
        setError(t('properties.form.errors.titleLength'));
        setLoading(false);
        return;
      }

      if (formData.description.length < 20 || formData.description.length > 5000) {
        setError(t('properties.form.errors.descriptionLength'));
        setLoading(false);
        return;
      }

      if (formData.address.length > 255) {
        setError(t('properties.form.errors.addressLength'));
        setLoading(false);
        return;
      }

      const bedrooms = Number(formData.bedrooms);
      const bathrooms = Number(formData.bathrooms);
      const area = Number(formData.area);
      const price = Number(formData.price);

      if (bedrooms < 0 || bedrooms > 50) {
        setError(t('properties.form.errors.bedroomsRange'));
        setLoading(false);
        return;
      }

      if (bathrooms < 0 || bathrooms > 50) {
        setError(t('properties.form.errors.bathroomsRange'));
        setLoading(false);
        return;
      }

      if (area < 1 || area > 100000) {
        setError(t('properties.form.errors.areaRange'));
        setLoading(false);
        return;
      }

      if (price <= 0) {
        setError(t('properties.form.errors.pricePositive'));
        setLoading(false);
        return;
      }

      let newImageUrls: string[] = [];
      if (imageUpload.images.length > 0) {
        try {
          newImageUrls = await imageUpload.uploadImages();
        } catch (uploadError) {
          setError(t('properties.form.errors.uploadFailed'));
          setLoading(false);
          return;
        }
      }

      const allImages = [...existingImages, ...newImageUrls];

      if (mode === 'edit' && propertyId) {
        await propertyApi.update(Number(propertyId), {
          title: formData.title,
          description: formData.description,
          price: Number(formData.price),
          address: formData.address,
          city: formData.city,
          bedrooms: Number(formData.bedrooms),
          bathrooms: Number(formData.bathrooms),
          area: Number(formData.area),
          propertyType: mapUiPropertyTypeToApi(formData.propertyType),
          status: mapUiPropertyStatusToApi(formData.status),
          features: selectedFeatures,
          images: allImages,
        }, {
          accessToken: session?.accessToken,
          user: session?.user ? {
            id: session.user.id as string,
            email: session.user.email,
            name: session.user.name,
            roles: ['ROLE_USER'],
          } : undefined,
        });

        router.push(`/properties/${propertyId}`);
        router.refresh();
      } else {
        const response = await fetch('/api/properties', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            ...formData,
            features: selectedFeatures,
            images: allImages,
          }),
        });

        const data = await response.json();

        if (!response.ok) {
          setError(data.error || t('properties.form.errors.general'));
          setLoading(false);
          return;
        }

        router.push('/');
        router.refresh();
      }
    } catch (err) {
      setError(t('properties.form.errors.general'));
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
          {t('properties.form.title')} <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          id="title"
          required
          minLength={10}
          maxLength={255}
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          placeholder={t('properties.form.titlePlaceholder')}
        />
        <p className="mt-1 text-sm text-gray-500">
          {t('properties.form.titleCounter', { current: formData.title.length })}
        </p>
      </div>

      <div>
        <label htmlFor="description" className="block text-sm font-medium text-gray-700">
          {t('properties.form.description')} <span className="text-red-500">*</span>
        </label>
        <textarea
          id="description"
          required
          minLength={20}
          maxLength={5000}
          rows={4}
          value={formData.description}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          placeholder={t('properties.form.descriptionPlaceholder')}
        />
        <p className="mt-1 text-sm text-gray-500">
          {t('properties.form.descriptionCounter', { current: formData.description.length })}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label htmlFor="propertyType" className="block text-sm font-medium text-gray-700">
            {t('properties.form.type')} <span className="text-red-500">*</span>
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
            <option value="house">{t('common.propertyType.house')}</option>
            <option value="apartment">{t('common.propertyType.apartment')}</option>
            <option value="villa">{t('common.propertyType.villa')}</option>
            <option value="townhouse">{t('common.propertyType.townhouse')}</option>
          </select>
        </div>

        <div>
          <label htmlFor="status" className="block text-sm font-medium text-gray-700">
            {t('properties.form.status')} <span className="text-red-500">*</span>
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
            <option value="for-sale">{t('common.propertyStatus.forSale')}</option>
            <option value="for-rent">{t('common.propertyStatus.forRent')}</option>
          </select>
        </div>
      </div>

      <div>
        <label htmlFor="price" className="block text-sm font-medium text-gray-700">
          {t('properties.form.price')} <span className="text-red-500">*</span>
        </label>
        <input
          type="number"
          id="price"
          required
          min="1"
          value={formData.price}
          onChange={(e) => setFormData({ ...formData, price: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          placeholder="5000000000"
        />
        <p className="mt-1 text-sm text-gray-500">
          {t('properties.form.priceHelp')}
        </p>
      </div>

      <div>
        <label htmlFor="address" className="block text-sm font-medium text-gray-700">
          {t('properties.form.address')} <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          id="address"
          required
          maxLength={255}
          value={formData.address}
          onChange={(e) => setFormData({ ...formData, address: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          placeholder={t('properties.form.addressPlaceholder')}
        />
        <p className="mt-1 text-sm text-gray-500">
          {t('properties.form.addressCounter', { current: formData.address.length })}
        </p>
      </div>

      <div>
        <label htmlFor="city" className="block text-sm font-medium text-gray-700">
          {t('properties.form.city')} <span className="text-red-500">*</span>
        </label>
        <select
          id="city"
          required
          value={formData.city}
          onChange={(e) => setFormData({ ...formData, city: e.target.value })}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
        >
          <option value="">{t('properties.form.selectCity')}</option>
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
            {t('properties.form.bedrooms')} <span className="text-red-500">*</span>
          </label>
          <input
            type="number"
            id="bedrooms"
            required
            min="0"
            max="50"
            value={formData.bedrooms}
            onChange={(e) => setFormData({ ...formData, bedrooms: e.target.value })}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          />
          <p className="mt-1 text-sm text-gray-500">
            {t('properties.form.roomRange')}
          </p>
        </div>

        <div>
          <label htmlFor="bathrooms" className="block text-sm font-medium text-gray-700">
            {t('properties.form.bathrooms')} <span className="text-red-500">*</span>
          </label>
          <input
            type="number"
            id="bathrooms"
            required
            min="0"
            max="50"
            value={formData.bathrooms}
            onChange={(e) => setFormData({ ...formData, bathrooms: e.target.value })}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          />
          <p className="mt-1 text-sm text-gray-500">
            {t('properties.form.roomRange')}
          </p>
        </div>

        <div>
          <label htmlFor="area" className="block text-sm font-medium text-gray-700">
            {t('properties.form.area')} <span className="text-red-500">*</span>
          </label>
          <input
            type="number"
            id="area"
            required
            min="1"
            max="100000"
            value={formData.area}
            onChange={(e) => setFormData({ ...formData, area: e.target.value })}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          />
          <p className="mt-1 text-sm text-gray-500">
            {t('properties.form.areaRange')}
          </p>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          {t('properties.form.features')}
        </label>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
          {FEATURE_KEYS.map((feature) => (
            <label key={feature} className="flex items-center">
              <input
                type="checkbox"
                checked={selectedFeatures.includes(feature)}
                onChange={() => handleFeatureToggle(feature)}
                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
              />
              <span className="ml-2 text-sm text-gray-700">
                {t(`properties.form.featuresList.${feature}` as any)}
              </span>
            </label>
          ))}
        </div>
      </div>

      <ImageUpload
        imageUpload={imageUpload}
        existingImages={existingImages}
        onRemoveExisting={handleRemoveExistingImage}
      />

      <div className="flex gap-4">
        <button
          type="submit"
          disabled={loading}
          className="flex-1 bg-primary-600 text-white py-2 px-4 rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading
            ? mode === 'edit'
              ? t('properties.form.updating')
              : t('properties.form.creating')
            : mode === 'edit'
            ? t('properties.form.update')
            : t('properties.form.create')}
        </button>
        <button
          type="button"
          onClick={() => router.back()}
          className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
        >
          {t('properties.form.cancel')}
        </button>
      </div>
    </form>
  );
}
