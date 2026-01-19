import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import Header from '@/components/Header';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import SessionProvider from '@/components/SessionProvider';

const inter = Inter({ subsets: ['latin', 'vietnamese'] });

export const metadata: Metadata = {
  title: 'BĐS Vietnam - Mua bán & Cho thuê Bất động sản',
  description: 'Nền tảng mua bán và cho thuê bất động sản hàng đầu Việt Nam. Tìm kiếm hàng trăm bất động sản từ nhà phố, căn hộ đến biệt thự.',
  keywords: 'bất động sản, nhà đất, mua bán nhà, cho thuê nhà, căn hộ, biệt thự',
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await getServerSession(authOptions);

  return (
    <html lang="vi">
      <body className={inter.className}>
        <SessionProvider session={session}>
          <Header />
          <main className="min-h-screen">
            {children}
          </main>
          <footer className="bg-gray-800 text-white py-8 mt-12">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
              <div className="text-center">
                <p className="text-lg font-semibold mb-2">BĐS Vietnam</p>
                <p className="text-sm text-gray-400">
                  Nền tảng mua bán & cho thuê bất động sản hàng đầu
                </p>
                <p className="text-sm text-gray-400 mt-4">
                  © 2026 BĐS Vietnam. All rights reserved.
                </p>
              </div>
            </div>
          </footer>
        </SessionProvider>
      </body>
    </html>
  );
}
