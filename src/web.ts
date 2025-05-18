import { WebPlugin } from '@capacitor/core';

import type { CapacitorMapSdkPlugin } from './definitions';

export class CapacitorMapSdkWeb extends WebPlugin implements CapacitorMapSdkPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
