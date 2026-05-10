'use client';

import { useEffect, useState } from 'react';
import { AiSearchParseResponse } from '@/types/api';
import InterpretedChips from './InterpretedChips';

const AI_SEARCH_ENABLED = process.env.NEXT_PUBLIC_AI_SEARCH === '1';

export default function AiSearchChipsBanner() {
  const [parsed, setParsed] = useState<AiSearchParseResponse | null>(null);

  useEffect(() => {
    if (!AI_SEARCH_ENABLED) return;
    try {
      const raw = sessionStorage.getItem('aiSearch:lastParse');
      if (raw) {
        setParsed(JSON.parse(raw) as AiSearchParseResponse);
      }
    } catch {
      /* ignore parse errors */
    }
  }, []);

  if (!AI_SEARCH_ENABLED || !parsed || !parsed.chips || parsed.chips.length === 0) {
    return null;
  }

  return (
    <InterpretedChips
      chips={parsed.chips}
      parserMode={parsed.parserMode}
      cacheHit={parsed.cacheHit}
    />
  );
}
