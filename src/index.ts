import { registerPlugin } from '@capacitor/core';

import type { CapacitorMapSdkPlugin } from './definitions';

const CapacitorMapSdk = registerPlugin<CapacitorMapSdkPlugin>('CapacitorMapSdk', {
  web: () => import('./web').then((m) => new m.CapacitorMapSdkWeb()),
});

export * from './definitions';
export { CapacitorMapSdk };
