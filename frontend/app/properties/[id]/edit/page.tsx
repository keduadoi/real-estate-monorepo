import { redirect, notFound } from 'next/navigation';
import { getServerSession } from 'next-auth';
import { getTranslations } from 'next-intl/server';
import { authOptions } from '@/lib/auth';
import PropertyForm from '@/components/PropertyForm';
import { propertyApi } from '@/lib/api/propertyApi';
import { mapApiPropertyToUi } from '@/lib/api/mapper';
import Link from 'next/link';

interface EditPropertyPageProps {
  params: {
    id: string;
  };
}

export default async function EditPropertyPage({ params }: EditPropertyPageProps) {
  const session = await getServerSession(authOptions);
  const t = await getTranslations('properties.editPage');

  if (!session) {
    redirect(`/login?callbackUrl=/properties/${params.id}/edit`);
  }

  let property;

  try {
    const apiProperty = await propertyApi.getById(Number(params.id));
    property = mapApiPropertyToUi(apiProperty);
  } catch (error) {
    notFound();
  }

  if (property.userId !== session.user?.id) {
    redirect(`/properties/${params.id}`);
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6">
        <Link
          href={`/properties/${params.id}`}
          className="text-primary-600 hover:text-primary-700 text-sm font-medium inline-flex items-center"
        >
          <svg
            className="w-4 h-4 mr-1"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 19l-7-7 7-7"
            />
          </svg>
          {t('back')}
        </Link>
      </div>

      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          {t('heading')}
        </h1>
        <p className="text-gray-600">
          {t('subheading')}
        </p>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <PropertyForm
          mode="edit"
          initialData={property}
          propertyId={params.id}
        />
      </div>
    </div>
  );
}
