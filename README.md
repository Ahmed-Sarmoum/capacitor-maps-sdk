# Capacitor Maps SDK

A powerful and customizable Capacitor plugin for integrating Google Maps into your Android apps. Designed to support dynamic API keys, custom marker rendering (including icons and color schemes), camera controls, location services, and comprehensive event listeners for all map interactions.

## ✨ Features

- 📍 Add default or custom markers (with `mdiIcon` or base64 `iconImage`)
- 🎨 Fully stylable custom markers using color sets
- 🔄 Optional draggable support for all markers
- 🎯 Move and control the camera programmatically with animation support
- 📍 Built-in location services with custom location button
- 🎭 Real-time marker interactions: Click, drag start, drag, and drag end events
- 🗺️ Map interaction events: Click and bounds changed
- 🔧 Dynamic map bounds updates
- ⚡ Enable/disable map interaction on demand
- 🎮 Zoom limits configuration
- 🧹 Clear all markers at once
- 🗑️ Destroy and clean up map from view

## 📁 Install

```bash
npm install capacitor-maps-sdk
npx cap sync
```

## 🔧 Configuration

Add the Google Maps API key to your `android/app/src/main/AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY" />
```

Add location permissions if using location features:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## 📃 API Reference

### initialize

Initializes the map with Google Maps API key and configuration options.

```ts
initialize(options: {
  apiKey: string;
  containerId?: string;
  showLocationButton?: boolean;
  locationButtonPosition?: {
    left?: number;
    right?: number;
    top?: number;
    bottom?: number;
  };
}): Promise<void>
```

**Parameters:**

- `apiKey`: Your Google Maps API key
- `containerId`: HTML container ID (default: "map-container")
- `showLocationButton`: Show current location button (default: false)
- `locationButtonPosition`: Position of location button in pixels

### addMarker

Adds a simple marker with optional title and drag capability.

```ts
addMarker(options: {
  latitude: number;
  longitude: number;
  title?: string;
  draggable?: boolean;
}): Promise<{ markerId: string }>
```

### addCustomMarker

Adds a custom-styled marker using a Material Design Icon or base64 image.

```ts
addCustomMarker(options: {
  position: { latitude: number; longitude: number };
  mdiIcon?: string;
  iconImage?: string;
  colors?: string[];
  title?: string;
  draggable?: boolean;

}): Promise<{ markerId: string }>
```

**Parameters:**

- `position`: Marker coordinates
- `mdiIcon`: Material Design Icon identifier
- `iconImage`: Base64 encoded PNG image (format: "data:image/png;base64,...")
- `colors`: An array of three color strings (e.g., `['#RRGGBB', '#RRGGBB', '#RRGGBB']`) used to customize the appearance of the marker when `mdiIcon` is provided. The colors correspond to:
  - Index 0: Outer circle and pin tip color
  - Index 1: Inner circle color
  - Index 2: MDI icon color
    Note: Custom markers using MDI icons require the `mdi.ttf` font file to be present in `android/app/src/main/assets/fonts/`.
- `draggable`: Enable marker dragging

### moveCamera / moveToPosition

Moves the camera to a specific position with optional zoom and animation.

```ts
moveToPosition(options: {
  latitude: number;
  longitude: number;
  zoom?: number;
  animate?: boolean;
}): Promise<{ latitude: number; longitude: number; zoom: number }>
```

```ts
moveCamera(options: {
  latitude: number;
  longitude: number;
  zoom?: number;
}): Promise<void>
```

### isReady

Checks if the map is fully initialized and ready for use.

```ts
isReady(): Promise<{
  isReady: boolean;
  mapReady: boolean;
  mapViewReady: boolean;
  containerReady: boolean;
  status: string;
  message: string;
}>
```

**Returns:**

- `isReady`: Overall status (true if all components are ready)
- `mapReady`: True if the GoogleMap instance is available
- `mapViewReady`: True if the MapView is initialized
- `containerReady`: True if the map container is created
- `status`: "ready" or "not_ready"
- `message`: Detailed status message

### Location Services

#### getCurrentLocation

Gets the current device location and moves camera to it.

```ts
getCurrentLocation(): Promise<{
  latitude: number;
  longitude: number;
  accuracy: number;
}>
```

#### toggleLocationButton

Shows or hides the current location button.

```ts
toggleLocationButton(options: {
  show: boolean;
}): Promise<{ visible: boolean }>
```

### Map Control

#### updateMapBounds

Updates the map container position and size.

```ts
updateMapBounds(options: {
  x: number;
  y: number;
  width: number;
  height: number;
}): Promise<void>
```

#### enableMapInteraction

Enables map touch interactions (brings map to foreground).

```ts
enableMapInteraction(): Promise<void>
```

#### disableMapInteraction

Disables map touch interactions (sends map to background).

```ts
disableMapInteraction(): Promise<void>
```

#### setZoomLimits

Sets minimum and maximum zoom levels.

```ts
setZoomLimits(options: {
  minZoom?: number;
  maxZoom?: number;
}): Promise<void>
```

### Marker Management

#### clearMarkers

Clears all currently displayed markers from the map.

```ts
clearMarkers(): Promise<{
  cleared: boolean;
  message: string;
}>
```

Removes all markers from the map except those whose title is included in the provided titles array

```ts
clearExpectMarkers(options: { titles: string[] }): Promise<void>;
```

**Parameters:**

- `titles`: A list of marker titles that should remain on the map

### Cleanup

#### destroyMap

Completely removes the map and frees native resources.

```ts
destroyMap(): Promise<void>
```

## 🎧 Event Listeners

### Marker Events

#### onMarkerClick

Triggered when a marker is clicked.

```ts
addListener('onMarkerClick', (data: {
  mapId: string;
  markerId: string;
  latitude: number;
  longitude: number;
  title?: string;
  screenX: number;
  screenY: number;
  mapX: number;
  mapY: number;
}) => void): Promise<{ remove: () => void }>
```

#### onMarkerDragStart

Triggered when marker dragging starts.

```ts
addListener('onMarkerDragStart', (data: {
  mapId: string;
  markerId: string;
  latitude: number;
  longitude: number;
  title?: string;
}) => void): Promise<{ remove: () => void }>
```

#### onMarkerDrag

Triggered during marker dragging.

```ts
addListener('onMarkerDrag', (data: {
  mapId: string;
  markerId: string;
  latitude: number;
  longitude: number;
  title?: string;
}) => void): Promise<{ remove: () => void }>
```

#### onMarkerDragEnd

Triggered when marker dragging ends (includes reverse geocoding).

```ts
addListener('onMarkerDragEnd', (data: {
  mapId: string;
  markerId: string;
  latitude: number;
  longitude: number;
  title?: string;
  address?: string;
}) => void): Promise<{ remove: () => void }>
```

### Map Events

#### onMapClick

Triggered when the map is clicked (not on a marker).

```ts
addListener('onMapClick', (data: {
  mapId: string;
  latitude: number;
  longitude: number;
}) => void): Promise<{ remove: () => void }>
```

#### onBoundsChanged

Triggered when the map camera moves or zoom changes.

```ts
addListener('onBoundsChanged', (data: {
  north: number;
  south: number;
  east: number;
  west: number;
  center_lat: number;
  center_lng: number;
}) => void): Promise<{ remove: () => void }>
```

#### onLocationFound

Triggered when current location is successfully retrieved.

```ts
addListener('onLocationFound', (data: {
  latitude: number;
  longitude: number;
  accuracy: number;
}) => void): Promise<{ remove: () => void }>
```

## 💡 Interfaces

### MarkerEventData

```ts
interface MarkerEventData {
  latitude: number;
  longitude: number;
  title?: string;
  address?: string;
}
```

### Position

```ts
interface Position {
  latitude: number;
  longitude: number;
}
```

## ✨ Example Usage

### Basic Setup

```ts
import { CapacitorMapSdk } from 'capacitor-maps-sdk';

// Initialize map with location button
await CapacitorMapSdk.initialize({
  apiKey: 'YOUR_GOOGLE_MAPS_API_KEY',
  containerId: 'map-container',
  showLocationButton: true,
  locationButtonPosition: {
    right: 16,
    bottom: 100,
  },
});
```

### Adding Markers

```ts
// Add simple marker
await CapacitorMapSdk.addMarker({
  latitude: 36.75,
  longitude: 3.06,
  title: 'Algiers',
  draggable: true,
});

// Add custom marker with MDI icon
await CapacitorMapSdk.addCustomMarker({
  position: { latitude: 36.75, longitude: 3.06 },
  mdiIcon: '🏢', // Material Design Icon (e.g., '🏢' for building)
  colors: ['#F44336', '#FFFFFF', '#FFC107'], // [outerCircleColor, innerCircleColor, iconColor]
  draggable: true,
});

// Add custom marker with base64 image
await CapacitorMapSdk.addCustomMarker({
  position: { latitude: 36.75, longitude: 3.06 },
  iconImage: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgA...',
  draggable: false,
});
```

### Camera Control

```ts
// Move camera with animation
await CapacitorMapSdk.moveToPosition({
  latitude: 36.75,
  longitude: 3.06,
  zoom: 15,
  animate: true,
});

// Set zoom limits
await CapacitorMapSdk.setZoomLimits({
  minZoom: 10,
  maxZoom: 20,
});
```

### Location Services

```ts
// Get current location
try {
  const location = await CapacitorMapSdk.getCurrentLocation();
  console.log('Current location:', location);
} catch (error) {
  console.error('Location error:', error);
}

// Toggle location button
await CapacitorMapSdk.toggleLocationButton({ show: false });
```

### Event Handling

```ts
// Listen for marker clicks
const markerClickListener = await CapacitorMapSdk.addListener('onMarkerClick', (data) => {
  console.log('Marker clicked:', data.title, 'at', data.latitude, data.longitude);
  console.log('Screen position:', data.screenX, data.screenY);
});

// Listen for marker drag end (with address)
const dragEndListener = await CapacitorMapSdk.addListener('onMarkerDragEnd', (data) => {
  console.log('Marker moved to:', data.latitude, data.longitude);
  if (data.address) {
    console.log('Address:', data.address);
  }
});

// Listen for map bounds changes
const boundsListener = await CapacitorMapSdk.addListener('onBoundsChanged', (data) => {
  console.log('Map bounds:', data);
});

// Remove listeners when done
markerClickListener.remove();
dragEndListener.remove();
boundsListener.remove();
```

### Map Interaction Control

```ts
// Enable map interaction (user can pan/zoom)
await CapacitorMapSdk.enableMapInteraction();

// Disable map interaction (map goes to background)
await CapacitorMapSdk.disableMapInteraction();

// Update map bounds dynamically
await CapacitorMapSdk.updateMapBounds({
  x: 0,
  y: 100,
  width: 400,
  height: 600,
});
```

### Cleanup

```ts
// Clear all markers
await CapacitorMapSdk.clearMarkers();

// Destroy map when navigating away
await CapacitorMapSdk.destroyMap();
```

## 🎨 Custom Marker Generation

When using `addCustomMarker` with `mdiIcon` and `colors`, the plugin dynamically generates a marker bitmap on Android using the `generateMarkerBitmap` helper function. This function creates a layered icon with a circular base, an inner circle, a triangular pin tip, and the specified Material Design Icon.

The `colors` array (`[outerCircleColor, innerCircleColor, iconColor]`) directly controls the visual elements:

- The first color (`outerCircleColor`) is used for the outer circle of the marker and the triangular pin tip.
- The second color (`innerCircleColor`) fills the inner circle of the marker.
- The third color (`iconColor`) is applied to the `mdiIcon` itself.

This allows for highly customizable markers that match your application's branding or specific visual requirements.

## 🚧 Limitations

- Currently supports **Android only**
- No web implementation (throws error if used on web)
- Location services require appropriate permissions
- Custom markers using MDI icons require the `mdi.ttf` font file in `android/app/src/main/assets/fonts/`

## 📱 Android Setup

Ensure you have the following in your `android/app/build.gradle`:

```gradle
dependencies {
    implementation 'com.google.android.gms:play-services-maps:18.1.0'
    implementation 'com.google.android.gms:play-services-location:21.0.1'
}
```

## 🔍 Troubleshooting

### Location Button Not Showing

- Verify location permissions are granted
- Check that `showLocationButton: true` is set in initialization
- Ensure Google Play Services is available on the device

### Map Not Displaying

- Verify your Google Maps API key is correct and has Maps SDK for Android enabled
- Check that the API key is properly set in AndroidManifest.xml
- Ensure the container element exists in your HTML

### Markers Not Appearing

- Verify coordinates are valid (latitude: -90 to 90, longitude: -180 to 180)
- Check that the map is initialized before adding markers
- For custom markers, ensure the `mdi.ttf` font file exists

## 🙌 Contributions

PRs are welcome! Please open an issue first to discuss what you'd like to change.

## 📄 License

MIT © 2025
