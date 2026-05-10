'use client';

import Link from 'next/link';
import { useLocale, useTranslations } from 'next-intl';

interface Props {
  status: 'for-sale' | 'for-rent';
}

interface FilterItem {
  label: string;
  params: Record<string, string>;
}

// Prices are stored in millions (the /search page multiplies by 1e6).
// 500 → 500,000,000 VND ; 1000 → 1B ; 5000 → 5B etc.
const SALE_PRICE_VI: FilterItem[] = [
  { label: 'Dưới 500 triệu', params: { maxPrice: '500' } },
  { label: '500 - 800 triệu', params: { minPrice: '500', maxPrice: '800' } },
  { label: '800 triệu - 1 tỷ', params: { minPrice: '800', maxPrice: '1000' } },
  { label: '1 - 2 tỷ', params: { minPrice: '1000', maxPrice: '2000' } },
  { label: '2 - 3 tỷ', params: { minPrice: '2000', maxPrice: '3000' } },
  { label: '3 - 5 tỷ', params: { minPrice: '3000', maxPrice: '5000' } },
  { label: '5 - 7 tỷ', params: { minPrice: '5000', maxPrice: '7000' } },
  { label: '7 - 10 tỷ', params: { minPrice: '7000', maxPrice: '10000' } },
  { label: '10 - 20 tỷ', params: { minPrice: '10000', maxPrice: '20000' } },
  { label: 'Trên 20 tỷ', params: { minPrice: '20000' } },
];

const SALE_PRICE_EN: FilterItem[] = [
  { label: 'Under 500M', params: { maxPrice: '500' } },
  { label: '500M - 800M', params: { minPrice: '500', maxPrice: '800' } },
  { label: '800M - 1B', params: { minPrice: '800', maxPrice: '1000' } },
  { label: '1B - 2B', params: { minPrice: '1000', maxPrice: '2000' } },
  { label: '2B - 3B', params: { minPrice: '2000', maxPrice: '3000' } },
  { label: '3B - 5B', params: { minPrice: '3000', maxPrice: '5000' } },
  { label: '5B - 7B', params: { minPrice: '5000', maxPrice: '7000' } },
  { label: '7B - 10B', params: { minPrice: '7000', maxPrice: '10000' } },
  { label: '10B - 20B', params: { minPrice: '10000', maxPrice: '20000' } },
  { label: 'Over 20B', params: { minPrice: '20000' } },
];

const RENT_PRICE_VI: FilterItem[] = [
  { label: 'Dưới 5 triệu', params: { maxPrice: '5' } },
  { label: '5 - 10 triệu', params: { minPrice: '5', maxPrice: '10' } },
  { label: '10 - 20 triệu', params: { minPrice: '10', maxPrice: '20' } },
  { label: '20 - 40 triệu', params: { minPrice: '20', maxPrice: '40' } },
  { label: '40 - 70 triệu', params: { minPrice: '40', maxPrice: '70' } },
  { label: '70 - 100 triệu', params: { minPrice: '70', maxPrice: '100' } },
  { label: 'Trên 100 triệu', params: { minPrice: '100' } },
];

const RENT_PRICE_EN: FilterItem[] = [
  { label: 'Under 5M', params: { maxPrice: '5' } },
  { label: '5M - 10M', params: { minPrice: '5', maxPrice: '10' } },
  { label: '10M - 20M', params: { minPrice: '10', maxPrice: '20' } },
  { label: '20M - 40M', params: { minPrice: '20', maxPrice: '40' } },
  { label: '40M - 70M', params: { minPrice: '40', maxPrice: '70' } },
  { label: '70M - 100M', params: { minPrice: '70', maxPrice: '100' } },
  { label: 'Over 100M', params: { minPrice: '100' } },
];

const TYPE_ITEMS_VI: FilterItem[] = [
  { label: 'Nhà phố', params: { propertyType: 'townhouse' } },
  { label: 'Căn hộ', params: { propertyType: 'apartment' } },
  { label: 'Biệt thự', params: { propertyType: 'villa' } },
  { label: 'Nhà ở', params: { propertyType: 'house' } },
];

const TYPE_ITEMS_EN: FilterItem[] = [
  { label: 'Townhouse', params: { propertyType: 'townhouse' } },
  { label: 'Apartment', params: { propertyType: 'apartment' } },
  { label: 'Villa', params: { propertyType: 'villa' } },
  { label: 'House', params: { propertyType: 'house' } },
];

const BEDROOM_ITEMS_VI: FilterItem[] = [
  { label: '1 phòng ngủ', params: { bedrooms: '1' } },
  { label: '2 phòng ngủ', params: { bedrooms: '2' } },
  { label: '3 phòng ngủ', params: { bedrooms: '3' } },
  { label: '4+ phòng ngủ', params: { bedrooms: '4' } },
];

const BEDROOM_ITEMS_EN: FilterItem[] = [
  { label: '1 bedroom', params: { bedrooms: '1' } },
  { label: '2 bedrooms', params: { bedrooms: '2' } },
  { label: '3 bedrooms', params: { bedrooms: '3' } },
  { label: '4+ bedrooms', params: { bedrooms: '4' } },
];

const CITY_ITEMS: FilterItem[] = [
  { label: 'Hà Nội', params: { city: 'Hà Nội' } },
  { label: 'TP. Hồ Chí Minh', params: { city: 'TP. Hồ Chí Minh' } },
  { label: 'Đà Nẵng', params: { city: 'Đà Nẵng' } },
  { label: 'Hải Phòng', params: { city: 'Hải Phòng' } },
  { label: 'Cần Thơ', params: { city: 'Cần Thơ' } },
  { label: 'Nha Trang', params: { city: 'Nha Trang' } },
  { label: 'Vũng Tàu', params: { city: 'Vũng Tàu' } },
];

function buildHref(status: Props['status'], params: Record<string, string>): string {
  const sp = new URLSearchParams({ status, ...params });
  return `/search?${sp.toString()}`;
}

interface FilterListProps {
  title: string;
  items: FilterItem[];
  status: Props['status'];
}

function FilterList({ title, items, status }: FilterListProps) {
  return (
    <div className="mb-6">
      <h3 className="text-sm font-semibold text-gray-900 mb-2">{title}</h3>
      <ul className="space-y-1.5">
        {items.map((item) => {
          const key = item.label;
          return (
            <li key={key}>
              <Link
                href={buildHref(status, item.params)}
                className="text-sm text-gray-600 hover:text-primary-600 hover:underline"
              >
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export default function CommonFiltersSidebar({ status }: Props) {
  const t = useTranslations();
  const locale = useLocale();
  const isVi = locale.toLowerCase().startsWith('vi');

  const priceItems =
    status === 'for-sale'
      ? isVi ? SALE_PRICE_VI : SALE_PRICE_EN
      : isVi ? RENT_PRICE_VI : RENT_PRICE_EN;
  const typeItems = isVi ? TYPE_ITEMS_VI : TYPE_ITEMS_EN;
  const bedroomItems = isVi ? BEDROOM_ITEMS_VI : BEDROOM_ITEMS_EN;

  return (
    <aside className="bg-white rounded-lg border border-gray-200 p-5 sticky top-4">
      <FilterList title={t('commonFilters.byPrice')} items={priceItems} status={status} />
      <FilterList title={t('commonFilters.byType')} items={typeItems} status={status} />
      <FilterList title={t('commonFilters.byBedrooms')} items={bedroomItems} status={status} />
      <FilterList title={t('commonFilters.byCity')} items={CITY_ITEMS} status={status} />
    </aside>
  );
}
