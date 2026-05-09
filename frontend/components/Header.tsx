'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useSession, signOut } from 'next-auth/react';
import { useTranslations } from 'next-intl';
import LanguageSwitcher from './LanguageSwitcher';

export default function Header() {
  const { data: session } = useSession();
  const pathname = usePathname();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const t = useTranslations();

  const isActive = (href: string) =>
    href === '/' ? pathname === '/' : pathname.startsWith(href);

  const isAdmin = session?.user?.roles?.includes('ROLE_ADMIN') ?? false;

  // Close user menu when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setUserMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <header className="bg-white shadow-sm">
      <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <Link href="/" className="flex items-center">
              <span className="text-2xl font-bold text-primary-600">
                {t('brand.name')}
              </span>
            </Link>
          </div>

          {/* Desktop Navigation */}
          <div className="hidden md:flex md:items-center md:space-x-4">
            <Link
              href="/buy"
              className={`px-3 py-2 rounded-md text-sm font-medium ${isActive('/buy') ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-700 hover:text-primary-600'}`}
            >
              {t('nav.buy')}
            </Link>
            <Link
              href="/rent"
              className={`px-3 py-2 rounded-md text-sm font-medium ${isActive('/rent') ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-700 hover:text-primary-600'}`}
            >
              {t('nav.rent')}
            </Link>
            <Link
              href="/feed"
              className={`px-3 py-2 rounded-md text-sm font-medium ${isActive('/feed') ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-700 hover:text-primary-600'}`}
            >
              {t('nav.feed')}
            </Link>
            <Link
              href="/news"
              className={`px-3 py-2 rounded-md text-sm font-medium ${isActive('/news') ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-700 hover:text-primary-600'}`}
            >
              {t('nav.news')}
            </Link>
            <Link
              href="/search"
              className={`p-2 rounded-md ${isActive('/search') ? 'text-primary-600' : 'text-gray-700 hover:text-primary-600'}`}
              title={t('nav.search')}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </Link>

            {session ? (
              <>
                {isAdmin && (
                  <Link
                    href="/admin/news"
                    className={`px-3 py-2 rounded-md text-sm font-medium ${isActive('/admin') ? 'text-amber-600 border-b-2 border-amber-600' : 'text-amber-700 hover:text-amber-800'}`}
                  >
                    {t('nav.admin')}
                  </Link>
                )}
                <Link
                  href="/properties/new"
                  className="bg-primary-600 text-white hover:bg-primary-700 px-4 py-2 rounded-md text-sm font-medium"
                >
                  {t('nav.postListing')}
                </Link>
                <div
                  className="relative"
                  ref={userMenuRef}
                  onMouseEnter={() => setUserMenuOpen(true)}
                  onMouseLeave={() => setUserMenuOpen(false)}
                >
                  <button
                    className="flex items-center space-x-1 text-sm text-gray-700 hover:text-primary-600 px-3 py-2 rounded-md font-medium"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    <span>{session.user?.name}</span>
                    <svg className={`w-4 h-4 transition-transform ${userMenuOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>
                  {userMenuOpen && (
                    <div className="absolute right-0 mt-0 w-48 bg-white rounded-md shadow-lg py-1 z-50 border border-gray-100">
                      <button
                        onClick={() => {
                          setUserMenuOpen(false);
                          signOut({ callbackUrl: '/' });
                        }}
                        className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-primary-600 flex items-center space-x-2"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                        </svg>
                        <span>{t('auth.signOut')}</span>
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-2">
                <Link
                  href="/register"
                  className="text-gray-700 hover:text-primary-600 px-3 py-2 rounded-md text-sm font-medium"
                >
                  {t('auth.signUp')}
                </Link>
                <Link
                  href="/login"
                  className="bg-primary-600 text-white hover:bg-primary-700 px-4 py-2 rounded-md text-sm font-medium"
                >
                  {t('auth.signIn')}
                </Link>
              </div>
            )}

            <LanguageSwitcher />
          </div>

          {/* Mobile menu button */}
          <div className="md:hidden flex items-center">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="inline-flex items-center justify-center p-2 rounded-md text-gray-700 hover:text-primary-600 hover:bg-gray-100 focus:outline-none"
            >
              <svg
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                {mobileMenuOpen ? (
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                ) : (
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M4 6h16M4 12h16M4 18h16"
                  />
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {mobileMenuOpen && (
          <div className="md:hidden pb-3 pt-2 space-y-1">
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
              href="/feed"
              className={`block px-3 py-2 rounded-md text-base font-medium ${isActive('/feed') ? 'text-primary-600 bg-primary-50' : 'text-gray-700 hover:text-primary-600 hover:bg-gray-50'}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              {t('nav.feed')}
            </Link>
            <Link
              href="/news"
              className={`block px-3 py-2 rounded-md text-base font-medium ${isActive('/news') ? 'text-primary-600 bg-primary-50' : 'text-gray-700 hover:text-primary-600 hover:bg-gray-50'}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              {t('nav.news')}
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
                  className="block text-primary-600 hover:bg-primary-50 px-3 py-2 rounded-md text-base font-medium"
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

            <div className="px-3 py-2">
              <LanguageSwitcher />
            </div>
          </div>
        )}
      </nav>
    </header>
  );
}
