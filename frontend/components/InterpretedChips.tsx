'use client';

import { useTranslations } from 'next-intl';
import { AiSearchChip, AiParserMode } from '@/types/api';

interface InterpretedChipsProps {
  chips: AiSearchChip[];
  parserMode: AiParserMode;
  cacheHit?: boolean;
  onRemove?: (chipId: string) => void;
}

export default function InterpretedChips({
  chips,
  parserMode,
  cacheHit,
  onRemove,
}: InterpretedChipsProps) {
  const t = useTranslations();

  if (!chips || chips.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center gap-2 mb-4">
      <span className="text-sm text-gray-600 mr-1">{t('aiSearch.interpretedAs')}</span>
      {chips.map((chip) => (
        <span
          key={chip.id}
          className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium ${
            chip.confidence === 'low'
              ? 'bg-yellow-50 text-yellow-800 border border-yellow-200'
              : chip.confidence === 'medium'
              ? 'bg-blue-50 text-blue-800 border border-blue-200'
              : 'bg-green-50 text-green-800 border border-green-200'
          }`}
        >
          {chip.confidence === 'low' && (
            <span title={t('aiSearch.lowConfidence')} aria-label="low confidence">
              ⚠️
            </span>
          )}
          {chip.label}
          {onRemove && (
            <button
              type="button"
              onClick={() => onRemove(chip.id)}
              className="ml-1 text-gray-500 hover:text-gray-900 focus:outline-none"
              aria-label={`remove ${chip.label}`}
            >
              ×
            </button>
          )}
        </span>
      ))}
      <span className="ml-auto text-xs text-gray-400">
        {parserMode === 'llm' ? '✨ AI' : '⚡ regex'}
        {cacheHit && ' · cached'}
      </span>
    </div>
  );
}
