import { redirect } from 'next/navigation';

export const dynamic = 'force-dynamic';

interface HomePageProps {
  searchParams: Record<string, string | string[] | undefined>;
}

export default function HomePage({ searchParams }: HomePageProps) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(searchParams)) {
    if (typeof value === 'string') {
      params.set(key, value);
    } else if (Array.isArray(value)) {
      for (const v of value) params.append(key, v);
    }
  }
  const qs = params.toString();
  redirect(qs ? `/buy?${qs}` : '/buy');
}
