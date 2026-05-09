'use client';

import { useEffect, useRef, useState, useTransition } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { setLocale } from '@/lib/i18n/setLocale';
import { SUPPORTED_LOCALES, type Locale } from '@/i18n/config';

export default function LanguageSwitcher() {
  const currentLocale = useLocale() as Locale;
  const t = useTranslations('language');
  const [open, setOpen] = useState(false);
  const [pending, startTransition] = useTransition();
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (next: Locale) => {
    setOpen(false);
    if (next === currentLocale) return;
    startTransition(() => {
      setLocale(next);
    });
  };

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        disabled={pending}
        aria-label={t('label')}
        className="flex items-center space-x-1 text-sm text-gray-700 hover:text-primary-600 px-3 py-2 rounded-md font-medium uppercase disabled:opacity-50"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5h12M9 3v2m4.5 4l-3 9m-3-9l3 9m-3-3h6m6 4a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>{currentLocale}</span>
      </button>
      {open && (
        <div className="absolute right-0 mt-1 w-36 bg-white rounded-md shadow-lg py-1 z-50 border border-gray-100">
          {SUPPORTED_LOCALES.map((locale) => (
            <button
              key={locale}
              type="button"
              onClick={() => handleSelect(locale)}
              className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-100 flex items-center justify-between ${
                locale === currentLocale ? 'text-primary-600 font-medium' : 'text-gray-700'
              }`}
            >
              <span>{t(locale)}</span>
              {locale === currentLocale && (
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
