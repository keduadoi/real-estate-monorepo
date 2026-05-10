'use client';

import { useEffect, useState } from 'react';
import { useTranslations } from 'next-intl';
import AiSearchBar from './AiSearchBar';
import SearchBar from './SearchBar';

const AI_SEARCH_ENABLED = process.env.NEXT_PUBLIC_AI_SEARCH === '1';
const STORAGE_KEY = 'aiSearch:mode';

export default function SearchModeSwitcher() {
  const t = useTranslations();
  const [aiMode, setAiMode] = useState(false);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    if (!AI_SEARCH_ENABLED) return;
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'ai') setAiMode(true);
    } catch {
      /* ignore */
    }
    setHydrated(true);
  }, []);

  const toggle = (next: boolean) => {
    setAiMode(next);
    try {
      localStorage.setItem(STORAGE_KEY, next ? 'ai' : 'filter');
    } catch {
      /* ignore */
    }
  };

  if (!AI_SEARCH_ENABLED) {
    return <SearchBar />;
  }

  return (
    <div>
      <div className="flex justify-end mb-3">
        <label
          className="inline-flex items-center gap-2 cursor-pointer select-none"
          aria-label={t('aiSearch.toggleAria')}
        >
          <span className="text-sm font-medium text-gray-700">
            {t('aiSearch.toggleLabel')}{' '}
            <span className="text-xs uppercase font-semibold text-primary-600">
              ({t('aiSearch.beta')})
            </span>
          </span>
          <span className="relative inline-flex">
            <input
              type="checkbox"
              checked={aiMode}
              onChange={(e) => toggle(e.target.checked)}
              className="sr-only peer"
            />
            <span className="w-10 h-6 bg-gray-300 rounded-full transition-colors peer-checked:bg-primary-600" />
            <span className="absolute left-0.5 top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform peer-checked:translate-x-4" />
          </span>
        </label>
      </div>

      {hydrated && aiMode ? <AiSearchBar /> : <SearchBar />}
    </div>
  );
}
