import { getServerSession } from 'next-auth';
import { redirect } from 'next/navigation';
import { authOptions } from '@/lib/auth';

export async function requireAdmin() {
  const session = await getServerSession(authOptions);
  if (!session?.user) {
    redirect('/login?callbackUrl=/admin/news');
  }
  if (!session.user.roles?.includes('ROLE_ADMIN')) {
    redirect('/?error=forbidden');
  }
  return session;
}
