'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function NavigationBridge() {
  const router = useRouter();

  useEffect(() => {
    window.bublapiNavigate = (href, { replace = false } = {}) => {
      if (replace) router.replace(href);
      else router.push(href);
    };

    return () => {
      delete window.bublapiNavigate;
    };
  }, [router]);

  return null;
}
