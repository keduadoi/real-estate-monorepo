import type { Locale } from '@/i18n/config';

const INTL_LOCALES: Record<Locale, string> = {
  vi: 'vi-VN',
  en: 'en-US',
};

export function toIntlLocale(locale: string | undefined): string {
  if (locale && locale in INTL_LOCALES) {
    return INTL_LOCALES[locale as Locale];
  }
  return INTL_LOCALES.vi;
}
