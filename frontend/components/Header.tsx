'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useSession, signOut } from 'next-auth/react';
import { useTranslations } from 'next-intl';
import LanguageSwitcher from './LanguageSwitcher';

const PHONE = '+84 901 234 567';

interface NavDropdownItem {
  label: string;
  href: string;
}

export default function Header() {
  const { data: session } = useSession();
  const pathname = usePathname();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [openDropdown, setOpenDropdown] = useState<'buy' | 'rent' | null>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const t = useTranslations();

  const isActive = (href: string) =>
    href === '/' ? pathname === '/' : pathname.startsWith(href);

  const isAdmin = session?.user?.roles?.includes('ROLE_ADMIN') ?? false;

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setUserMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const buyMenu: NavDropdownItem[] = [
    { label: t('nav.allBuy'), href: '/buy' },
    { label: t('common.propertyType.apartment'), href: '/search?status=for-sale&propertyType=apartment' },
    { label: t('common.propertyType.house'), href: '/search?status=for-sale&propertyType=house' },
    { label: t('common.propertyType.villa'), href: '/search?status=for-sale&propertyType=villa' },
    { label: t('common.propertyType.townhouse'), href: '/search?status=for-sale&propertyType=townhouse' },
  ];
  const rentMenu: NavDropdownItem[] = [
    { label: t('nav.allRent'), href: '/rent' },
    { label: t('common.propertyType.apartment'), href: '/search?status=for-rent&propertyType=apartment' },
    { label: t('common.propertyType.house'), href: '/search?status=for-rent&propertyType=house' },
    { label: t('common.propertyType.villa'), href: '/search?status=for-rent&propertyType=villa' },
    { label: t('common.propertyType.townhouse'), href: '/search?status=for-rent&propertyType=townhouse' },
  ];

  const linkBase = 'text-sm font-medium transition-colors';
  const linkClass = (active: boolean) =>
    `${linkBase} px-1 py-2 ${
      active ? 'text-primary-600' : 'text-gray-700 hover:text-primary-600'
    }`;

  return (
    <header className="sticky top-0 z-40 bg-white border-b border-gray-200 shadow-sm">
      {/* ======= Utility bar ======= */}
      <div className="hidden md:block bg-gray-50 border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-9 flex items-center justify-between text-xs text-gray-600">
          <a href={`tel:${PHONE.replace(/\s/g, '')}`} className="flex items-center gap-1.5 hover:text-primary-600">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
            </svg>
            <span>{PHONE}</span>
          </a>

          <div className="flex items-center gap-4">
            {!session && (
              <div className="flex items-center gap-3">
                <Link href="/register" className="hover:text-primary-600">
                  {t('auth.signUp')}
                </Link>
                <span className="text-gray-300">|</span>
                <Link href="/login" className="hover:text-primary-600">
                  {t('auth.signIn')}
                </Link>
              </div>
            )}
            <LanguageSwitcher />
          </div>
        </div>
      </div>

      {/* ======= Main nav ======= */}
      <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex flex-col leading-tight shrink-0">
            <span className="text-xl md:text-2xl font-bold text-primary-600">
              {t('brand.name')}
            </span>
            <span className="hidden lg:block text-[11px] text-gray-500 -mt-0.5">
              {t('brand.tagline')}
            </span>
          </Link>

          {/* Desktop links */}
          <div className="hidden md:flex md:items-center md:gap-7 md:ml-10 flex-1">
            {/* Buy dropdown */}
            <div
              className="relative"
              onMouseEnter={() => setOpenDropdown('buy')}
              onMouseLeave={() => setOpenDropdown(null)}
            >
              <Link
                href="/buy"
                className={`${linkClass(isActive('/buy'))} flex items-center gap-1`}
              >
                {t('nav.buy')}
                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </Link>
              {openDropdown === 'buy' && (
                <div className="absolute left-0 top-full pt-2 z-50">
                  <div className="bg-white rounded-md shadow-lg border border-gray-100 py-1 w-52">
                    {buyMenu.map((item) => (
                      <Link
                        key={item.href}
                        href={item.href}
                        className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-primary-600"
                      >
                        {item.label}
                      </Link>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Rent dropdown */}
            <div
              className="relative"
              onMouseEnter={() => setOpenDropdown('rent')}
              onMouseLeave={() => setOpenDropdown(null)}
            >
              <Link
                href="/rent"
                className={`${linkClass(isActive('/rent'))} flex items-center gap-1`}
              >
                {t('nav.rent')}
                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </Link>
              {openDropdown === 'rent' && (
                <div className="absolute left-0 top-full pt-2 z-50">
                  <div className="bg-white rounded-md shadow-lg border border-gray-100 py-1 w-52">
                    {rentMenu.map((item) => (
                      <Link
                        key={item.href}
                        href={item.href}
                        className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-primary-600"
                      >
                        {item.label}
                      </Link>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <Link href="/news" className={linkClass(isActive('/news'))}>
              {t('nav.news')}
            </Link>
            <Link href="/feed" className={linkClass(isActive('/feed'))}>
              {t('nav.feed')}
            </Link>
          </div>

          {/* Right side */}
          <div className="hidden md:flex md:items-center md:gap-4">
            <Link
              href="/search"
              className={`p-2 rounded-md ${
                isActive('/search') ? 'text-primary-600' : 'text-gray-600 hover:text-primary-600'
              }`}
              title={t('nav.search')}
              aria-label={t('nav.search')}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </Link>

            {session && (
              <>
                {isAdmin && (
                  <Link
                    href="/admin/news"
                    className={`text-sm font-medium px-2 py-1 rounded ${
                      isActive('/admin')
                        ? 'text-amber-700 bg-amber-50'
                        : 'text-amber-700 hover:bg-amber-50'
                    }`}
                  >
                    {t('nav.admin')}
                  </Link>
                )}
                <Link
                  href="/properties/new"
                  className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-md text-sm font-semibold shadow-sm"
                >
                  {t('nav.postListing')}
                </Link>
                <div
                  className="relative"
                  ref={userMenuRef}
                  onMouseEnter={() => setUserMenuOpen(true)}
                  onMouseLeave={() => setUserMenuOpen(false)}
                >
                  <button className="flex items-center gap-1.5 text-sm text-gray-700 hover:text-primary-600 px-2 py-2 font-medium">
                    <span className="w-7 h-7 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-bold">
                      {(session.user?.name || '?').charAt(0).toUpperCase()}
                    </span>
                    <span className="hidden lg:inline max-w-[120px] truncate">
                      {session.user?.name}
                    </span>
                    <svg
                      className={`w-3.5 h-3.5 transition-transform ${
                        userMenuOpen ? 'rotate-180' : ''
                      }`}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>
                  {userMenuOpen && (
                    <div className="absolute right-0 top-full pt-2 w-48 z-50">
                      <div className="bg-white rounded-md shadow-lg py-1 border border-gray-100">
                        <button
                          onClick={() => {
                            setUserMenuOpen(false);
                            signOut({ callbackUrl: '/' });
                          }}
                          className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-primary-600 flex items-center gap-2"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                          </svg>
                          <span>{t('auth.signOut')}</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>

          {/* Mobile button */}
          <div className="md:hidden flex items-center">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="inline-flex items-center justify-center p-2 rounded-md text-gray-700 hover:text-primary-600 hover:bg-gray-100"
              aria-label="Toggle menu"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                {mobileMenuOpen ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {mobileMenuOpen && (
          <div className="md:hidden pb-3 pt-2 space-y-1 border-t border-gray-100">
            <Link
              href="/buy"
              className={`block px-3 py-2 rounded-md text-base font-medium ${isActive('/buy') ? 'text-primary-600 bg-primary-50' : 'text-gray-700 hover:text-primary-600 hover:bg-gray-50'}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              {t('nav.buy')}
            </Link>
            <Link
              href="/rent"
              className={`block px-3 py-2 rounded-md text-base font-medium ${isActive('/rent') ? 'text-primary-600 bg-primary-50' : 'text-gray-700 hover:text-primary-600 hover:bg-gray-50'}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              {t('nav.rent')}
            </Link>
            <Link
              href="/news"
              className={`block px-3 py-2 rounded-md text-base font-medium ${isActive('/news') ? 'text-primary-600 bg-primary-50' : 'text-gray-700 hover:text-primary-600 hover:bg-gray-50'}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              {t('nav.news')}
            </Link>
            <Link
              href="/feed"
              className={`block px-3 py-2 rounded-md text-base font-medium ${isActive('/feed') ? 'text-primary-600 bg-primary-50' : 'text-gray-700 hover:text-primary-600 hover:bg-gray-50'}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              {t('nav.feed')}
            </Link>
            <Link
              href="/search"
              className={`block px-3 py-2 rounded-md text-base font-medium ${isActive('/search') ? 'text-primary-600 bg-primary-50' : 'text-gray-700 hover:text-primary-600 hover:bg-gray-50'}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              {t('nav.search')}
            </Link>

            {session ? (
              <>
                {isAdmin && (
                  <Link
                    href="/admin/news"
                    className="block text-amber-700 hover:bg-amber-50 px-3 py-2 rounded-md text-base font-medium"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    {t('nav.admin')}
                  </Link>
                )}
                <Link
                  href="/properties/new"
                  className="block bg-primary-600 text-white px-3 py-2 rounded-md text-base font-medium text-center mx-3 my-2"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  {t('nav.postListing')}
                </Link>
                <div className="px-3 py-2 text-sm text-gray-700">
                  {session.user?.name}
                </div>
                <button
                  onClick={() => {
                    setMobileMenuOpen(false);
                    signOut({ callbackUrl: '/' });
                  }}
                  className="block w-full text-left text-gray-700 hover:text-primary-600 hover:bg-gray-50 px-3 py-2 rounded-md text-base font-medium"
                >
                  {t('auth.signOut')}
                </button>
              </>
            ) : (
              <>
                <Link
                  href="/register"
                  className="block text-gray-700 hover:text-primary-600 hover:bg-gray-50 px-3 py-2 rounded-md text-base font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  {t('auth.signUp')}
                </Link>
                <Link
                  href="/login"
                  className="block text-primary-600 hover:bg-primary-50 px-3 py-2 rounded-md text-base font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  {t('auth.signIn')}
                </Link>
              </>
            )}

            <div className="px-3 py-2 flex items-center justify-between border-t border-gray-100 mt-2 pt-3">
              <a href={`tel:${PHONE.replace(/\s/g, '')}`} className="text-sm text-gray-600 flex items-center gap-1.5">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
                {PHONE}
              </a>
              <LanguageSwitcher />
            </div>
          </div>
        )}
      </nav>
    </header>
  );
}
