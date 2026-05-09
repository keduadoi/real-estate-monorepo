import { Inter } from 'next/font/google';
import './globals.css';
import Header from '@/components/Header';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import SessionProvider from '@/components/SessionProvider';
import { NextIntlClientProvider } from 'next-intl';
import { getLocale, getMessages, getTranslations } from 'next-intl/server';

const inter = Inter({ subsets: ['latin', 'vietnamese'] });

export async function generateMetadata() {
  const t = await getTranslations('metadata');
  return {
    title: t('rootTitle'),
    description: t('rootDescription'),
  };
}

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await getServerSession(authOptions);
  const locale = await getLocale();
  const messages = await getMessages();
  const t = await getTranslations();

  return (
    <html lang={locale}>
      <body className={inter.className}>
        <NextIntlClientProvider locale={locale} messages={messages}>
          <SessionProvider session={session}>
            <Header />
            <main className="min-h-screen">
              {children}
            </main>
            <footer className="bg-gray-800 text-white py-8 mt-12">
              <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="text-center">
                  <p className="text-lg font-semibold mb-2">{t('brand.name')}</p>
                  <p className="text-sm text-gray-400">{t('brand.tagline')}</p>
                  <p className="text-sm text-gray-400 mt-4">
                    {t('footer.copyright', { year: new Date().getFullYear() })}
                  </p>
                </div>
              </div>
            </footer>
          </SessionProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
