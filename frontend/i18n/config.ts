export const SUPPORTED_LOCALES = ['vi', 'en'] as const;
export const DEFAULT_LOCALE: Locale = 'vi';
export const LOCALE_COOKIE = 'NEXT_LOCALE';

export type Locale = (typeof SUPPORTED_LOCALES)[number];

export function isLocale(value: string | undefined): value is Locale {
  return !!value && (SUPPORTED_LOCALES as readonly string[]).includes(value);
}
