import { WebPlugin } from '@capacitor/core';

import type { CapacitorMapSdkPlugin } from './definitions';

export class CapacitorMapSdkWeb extends WebPlugin implements CapacitorMapSdkPlugin {
  enableMapInteraction(): Promise<void> {
    throw new Error('Method not implemented.');
  }

  disableMapInteraction(): Promise<void> {
    throw new Error('Method not implemented.');
  }

  addCustomMarker(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }

  setZoomLimits(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }

  isReady(): Promise<{ value: boolean }> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }

  clearMarkers(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }
  clearExpectMarkers(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }

  async initialize(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }

  async addMarker(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }

  async updateMapBounds(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }

  async moveCamera(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }

  // // AddListener method implementation
  // // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
  // addListener(
  //   eventName: 'onMarkerClick' | 'onMarkerDrag' | 'onMarkerDragStart' | 'onMarkerDragEnd' | 'onMapClick' | 'onMapReady',
  //   listenerFunc: (event: MarkerEventData) => void,
  // ) {
  //   throw new Error(`addListener('${eventName}') is not implemented on web`);
  // }

  async destroyMap(): Promise<void> {
    throw new Error('CapacitorMapSdk is not implemented on web');
  }
}
