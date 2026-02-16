'use client';

import { useEffect, useState } from 'react';
import { PriceHistoryResponse, PricePointResponse } from '@/types/api';
import { formatPrice } from '@/lib/utils';
import { priceApi } from '@/lib/api/priceApi';

interface PriceHistoryProps {
  propertyId: number;
}

export default function PriceHistory({ propertyId }: PriceHistoryProps) {
  const [history, setHistory] = useState<PriceHistoryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchHistory() {
      try {
        const data = await priceApi.getPriceHistory(propertyId);
        setHistory(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load price history');
      } finally {
        setLoading(false);
      }
    }

    fetchHistory();
  }, [propertyId]);

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Lịch sử giá</h2>
        <div className="animate-pulse space-y-3">
          <div className="h-4 bg-gray-200 rounded w-3/4"></div>
          <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        </div>
      </div>
    );
  }

  if (error || !history || history.pricePoints.length === 0) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-xl font-semibold text-gray-900 mb-4">Lịch sử giá</h2>
      <div className="space-y-4">
        {history.pricePoints.map((point, index) => (
          <PriceChangeItem key={index} point={point} isFirst={index === 0} />
        ))}
      </div>
    </div>
  );
}

function PriceChangeItem({ point, isFirst }: { point: PricePointResponse; isFirst: boolean }) {
  const percentChange =
    point.oldPrice && point.oldPrice > 0
      ? ((point.newPrice - point.oldPrice) / point.oldPrice) * 100
      : null;

  const isIncrease = percentChange !== null && percentChange > 0;
  const isDecrease = percentChange !== null && percentChange < 0;

  return (
    <div className="flex items-start gap-3 relative">
      {/* Timeline dot and line */}
      <div className="flex flex-col items-center pt-1">
        <div
          className={`w-3 h-3 rounded-full flex-shrink-0 ${
            isIncrease ? 'bg-red-500' : isDecrease ? 'bg-green-500' : 'bg-blue-500'
          }`}
        />
        {!isFirst && <div className="w-0.5 h-full bg-gray-200 -mt-0" />}
      </div>

      {/* Content */}
      <div className="flex-1 pb-4 border-b border-gray-100 last:border-0">
        <div className="flex items-center gap-2 flex-wrap">
          {point.oldPrice ? (
            <>
              <span className="text-gray-500 line-through text-sm">
                {formatPrice(point.oldPrice)}
              </span>
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
              </svg>
              <span className="font-semibold text-gray-900">{formatPrice(point.newPrice)}</span>
            </>
          ) : (
            <span className="font-semibold text-gray-900">{formatPrice(point.newPrice)}</span>
          )}

          {percentChange !== null && (
            <span
              className={`text-xs font-medium px-1.5 py-0.5 rounded ${
                isIncrease
                  ? 'bg-red-100 text-red-700'
                  : isDecrease
                  ? 'bg-green-100 text-green-700'
                  : 'bg-gray-100 text-gray-700'
              }`}
            >
              {isIncrease ? '+' : ''}
              {percentChange.toFixed(1)}%
            </span>
          )}
        </div>

        <div className="mt-1 text-sm text-gray-500">
          <span>{new Date(point.changedAt).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}</span>
          {point.reason && (
            <span className="ml-2 text-gray-400">- {point.reason}</span>
          )}
        </div>
      </div>
    </div>
  );
}
