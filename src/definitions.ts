export interface MarkerEventData {
  latitude: number;
  longitude: number;
  title?: string;
  address?: string;
}

interface Position {
  latitude: number;
  longitude: number;
}

export interface CapacitorMapSdkPlugin {
  initialize(options: {
    apiKey: string;
    containerId: string;
    showLocationButton: boolean;
    locationButtonPosition?: {
      left: number;
      right: number;
      top: number;
      bottom: number;
    };
  }): Promise<void>;

  addMarker(options: { latitude: number; longitude: number; title?: string; draggable: boolean }): Promise<void>;

  addCustomMarker(options: {
    position: Position;
    mdiIcon?: string; // use mdi icon to generate default custom marker
    iconImage?: string; // optional base64 PNG
    colors?: string[]; // use this in case of using default custom marker
    draggable?: boolean;
  }): Promise<void>;

  moveCamera(options: { latitude: number; longitude: number; zoom?: number }): Promise<void>;

  updateMapBounds(options: { x: number; y: number; width: number; height: number }): Promise<void>;

  setZoomLimits(options: { maxZoom: number; minZoom: number }): Promise<void>;

  /**
   * Destroy the map and clean up resources
   * Should be called when navigating away from the map view
   */
  destroyMap(): Promise<void>;

  isReady(): Promise<boolean>;

  /**
   * Clear all markers from the map
   */
  clearMarkers(): Promise<void>;

  enableMapInteraction(): Promise<void>;

  disableMapInteraction(): Promise<void>;

  // addListener(eventName: 'onMarkerClick', listenerFunc: (event: MarkerEventData) => void): PluginListenerHandle;
  addListener(
    eventName:
      | 'onMarkerClick'
      | 'onMarkerDrag'
      | 'onMarkerDragStart'
      | 'onMarkerDragEnd'
      | 'onBoundsChanged'
      | 'onMapClick',
    listenerFunc: (data: any) => void,
  ): Promise<{ remove: () => void }>;
}
