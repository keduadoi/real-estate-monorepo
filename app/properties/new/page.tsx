import { redirect } from 'next/navigation';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import PropertyForm from '@/components/PropertyForm';

export default async function NewPropertyPage() {
  const session = await getServerSession(authOptions);

  if (!session) {
    redirect('/login?callbackUrl=/properties/new');
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Đăng tin bất động sản
        </h1>
        <p className="text-gray-600">
          Điền thông tin chi tiết về bất động sản của bạn
        </p>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <PropertyForm />
      </div>
    </div>
  );
}
