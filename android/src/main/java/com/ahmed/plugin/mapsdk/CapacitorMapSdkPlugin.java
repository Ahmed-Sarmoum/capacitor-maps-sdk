package com.ahmed.plugin.mapsdk;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

@CapacitorPlugin(name = "CapacitorMapSdk")
public class CapacitorMapSdkPlugin extends Plugin {

    private static final String MAPS_TAG = "CAPACITOR_MAPS_SDK_TAGS";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap googleMap;
    private MapView mapView;
    private Typeface mdiTypeface = null;
    private FrameLayout mapContainer = null;
    private String mapId = "default-map";
    private List<Marker> markers = new ArrayList<>();

    // Location services
    private FusedLocationProviderClient fusedLocationClient;
    private ImageButton currentLocationButton;
    private boolean showLocationButton = true;
    private PluginCall pendingLocationCall = null;

    // Map configuration
    private int mapX = 0;
    private int mapY = 0;
    private int mapWidth = 0;
    private int mapHeight = 0;

    @Override
    public void load() {
        super.load();
        if (mdiTypeface == null) {
            mdiTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/mdi.ttf");
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
    }

    @PluginMethod
    public void initialize(PluginCall call) throws PackageManager.NameNotFoundException {
        Context context = getContext();
        String apiKey = call.getString("apiKey");


        if (apiKey == null || apiKey.isEmpty()) {
            call.reject("API key is required");
            return;
        }

        // Store map configuration
        String containerId = call.getString("containerId", "map-container");
        showLocationButton = call.getBoolean("showLocationButton", false);

        // Get map dimensions and position from the container
        getActivity().runOnUiThread(() -> {
            View container = getActivity().findViewById(
                    context.getResources().getIdentifier(containerId, "id", context.getPackageName())
            );

            if (container != null) {
                // Get container bounds
                int[] location = new int[2];
                container.getLocationOnScreen(location);
                mapX = location[0];
                mapY = location[1];
                mapWidth = container.getWidth();
                mapHeight = container.getHeight();
            }


            initializeMap(apiKey, call);
        });
    }

    private void initializeMap(String apiKey, PluginCall call) {
        Context context = getContext();

        try {
            // Store API key in metadata
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA
            );
            Bundle bundle = appInfo.metaData;
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putString("com.google.android.geo.API_KEY", apiKey);

            if (mapView == null) {
                mapView = new MapView(context);
                mapView.onCreate(null);
                mapView.onResume();

                // Create a proper overlay container
                createMapOverlay();

                mapView.getMapAsync(gMap -> {
                    googleMap = gMap;

                    // Set up listeners
                    setupMapListeners();

                    // Create location button if enabled
                    if (showLocationButton) {
                        createLocationButton(call);
                    }

                    call.resolve();
                });
            } else {
                // Map already initialized, make sure it's visible
                if (mapView.getVisibility() != View.VISIBLE) {
                    mapView.setVisibility(View.VISIBLE);
                }
                call.resolve();
            }
        } catch (PackageManager.NameNotFoundException e) {
            call.reject("Failed to initialize map: " + e.getMessage());
        }
    }

    private void createMapOverlay() {
        Bridge bridge = getBridge();
        Context context = bridge.getContext();

        // Create map container
        mapContainer = new FrameLayout(context);
        mapContainer.setTag(mapId);

        // Set layout params for the map container
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        mapContainer.setLayoutParams(containerParams);

        // Create layout params for the map view
        FrameLayout.LayoutParams mapParams = new FrameLayout.LayoutParams(
                mapWidth > 0 ? getScaledPixels(bridge, mapWidth) : FrameLayout.LayoutParams.MATCH_PARENT,
                mapHeight > 0 ? getScaledPixels(bridge, mapHeight) : FrameLayout.LayoutParams.MATCH_PARENT
        );

        if (mapX > 0) mapParams.leftMargin = getScaledPixels(bridge, mapX);
        if (mapY > 0) mapParams.topMargin = getScaledPixels(bridge, mapY);

        mapView.setLayoutParams(mapParams);
        mapContainer.addView(mapView);

        // Add container to WebView parent
        ViewGroup webViewParent = (ViewGroup) bridge.getWebView().getParent();
        webViewParent.addView(mapContainer, 0); // Add at index 0 (behind WebView)

        // Make WebView background transparent so map shows through
        bridge.getWebView().setBackgroundColor(Color.TRANSPARENT);

        // Enable hardware acceleration for better performance
        mapView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mapContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void createLocationButton(PluginCall call) {
        JSObject locationButtonPosition = call.getObject("locationButtonPosition", null);

        Integer left = 0;
        Integer right = 0;
        Integer top = 0;
        Integer bottom = 0;

        if (locationButtonPosition != null) {
            if (locationButtonPosition.has("left")) {
                left = locationButtonPosition.getInteger("left");
            }
            if (locationButtonPosition.has("right")) {
                right = locationButtonPosition.getInteger("right");
            }
            if (locationButtonPosition.has("top")) {
                top = locationButtonPosition.getInteger("top");
            }
            if (locationButtonPosition.has("bottom")) {
                bottom = locationButtonPosition.getInteger("bottom");
            }
        }
        Context context = getContext();

        // Create the button
        currentLocationButton = new ImageButton(context);

        // Create custom colored background
        Drawable locationIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation);
        if (locationIcon != null) {
            locationIcon.setTint(Color.BLACK); // Black icon
        }

        // Set button style and icon
        currentLocationButton.setImageDrawable(locationIcon);
        currentLocationButton.setBackgroundColor(Color.WHITE); // White background
        currentLocationButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        currentLocationButton.setPadding(20, 20, 20, 20);

        // Add rounded corners and elevation for shadow
        currentLocationButton.setElevation(8f); // Shadow
        currentLocationButton.setStateListAnimator(null); // Remove default animation

        // Create rounded background drawable
        android.graphics.drawable.GradientDrawable roundedBackground = new android.graphics.drawable.GradientDrawable();
        roundedBackground.setColor(Color.WHITE);
        roundedBackground.setCornerRadius(24f); // Rounded corners (adjust as needed)
        roundedBackground.setStroke(1, Color.parseColor("#E0E0E0")); // Optional subtle border

        // Set the rounded background
        currentLocationButton.setBackground(roundedBackground);

        // Set button size and position
        int buttonSize = getScaledPixels(getBridge(), 48);
        int margin = getScaledPixels(getBridge(), 16);

        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                buttonSize, buttonSize
        );

        // Position to bottom right
        buttonParams.gravity = Gravity.BOTTOM | Gravity.END;
        buttonParams.setMargins(left, top,  right, bottom + 200);

        currentLocationButton.setLayoutParams(buttonParams);

        // Set click listener
        currentLocationButton.setOnClickListener(v -> getCurrentLocation());

        // Add button to map container
        mapContainer.addView(currentLocationButton);


    }

    @PluginMethod
    public void toggleLocationButton(PluginCall call) {
        boolean show = call.getBoolean("show", true);

        getActivity().runOnUiThread(() -> {
            if (currentLocationButton != null) {
                currentLocationButton.setVisibility(show ? View.VISIBLE : View.GONE);
                showLocationButton = show;

                JSObject result = new JSObject();
                result.put("visible", show);
                call.resolve(result);
            } else {
                call.reject("Location button not initialized");
            }
        });
    }

    @PluginMethod
    public void getCurrentLocation(PluginCall call) {
        pendingLocationCall = call;
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        // Check location permission
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Request permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Get current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // Move camera to current location
                        getActivity().runOnUiThread(() -> {
                            if (googleMap != null) {
                                LatLng currentLatLng = new LatLng(latitude, longitude);
                                googleMap.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f)
                                );

                                // Notify listeners
                                JSObject data = new JSObject();
                                data.put("latitude", latitude);
                                data.put("longitude", longitude);
                                data.put("accuracy", location.getAccuracy());
                                notifyListeners("onLocationFound", data);

                                // Resolve pending call if exists
                                if (pendingLocationCall != null) {
                                    pendingLocationCall.resolve(data);
                                    pendingLocationCall = null;
                                }
                            }
                        });
                    } else {
                        if (pendingLocationCall != null) {
                            pendingLocationCall.reject("Unable to get current location");
                            pendingLocationCall = null;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (pendingLocationCall != null) {
                        pendingLocationCall.reject("Failed to get location: " + e.getMessage());
                        pendingLocationCall = null;
                    }
                });
    }

    @PluginMethod
    public void moveToPosition(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (googleMap == null) {
                call.reject("Map not ready");
                return;
            }

            double lat = call.getDouble("latitude", 0.0);
            double lng = call.getDouble("longitude", 0.0);
            float zoom = call.getFloat("zoom", 15.0F);
            boolean animate = call.getBoolean("animate", true);
            LatLng position = new LatLng(lat, lng);

            if (animate) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
            }

            JSObject result = new JSObject();
            result.put("latitude", lat);
            result.put("longitude", lng);
            result.put("zoom", zoom);
            call.resolve(result);
        });
    }

    @PluginMethod
    public void updateMapBounds(PluginCall call) {
       getActivity().runOnUiThread(() -> {
            if (mapContainer == null) {
                call.reject("Map not initialized");
                return;
            }

            // Get new bounds
            int x = call.getInt("x", mapX);
            int y = call.getInt("y", mapY);
            int width = call.getInt("width", mapWidth);
            int height = call.getInt("height", mapHeight);

            Bridge bridge = getBridge();

            // Update stored values
            mapX = x;
            mapY = y;
            mapWidth = width;
            mapHeight = height;

            // Update map position and size
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mapView.getLayoutParams();
            layoutParams.leftMargin = getScaledPixels(bridge, x);
            layoutParams.topMargin = getScaledPixels(bridge, y);
            layoutParams.width = getScaledPixels(bridge, width);
            layoutParams.height = getScaledPixels(bridge, height);

            mapView.setLayoutParams(layoutParams);
            mapView.requestLayout();

            call.resolve();
        });
    }

    @PluginMethod
    public void destroyMap(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (mapContainer != null) {
                ViewGroup webViewParent = (ViewGroup) getBridge().getWebView().getParent();
                View viewToRemove = webViewParent.findViewWithTag(mapId);
                if (viewToRemove != null) {
                    webViewParent.removeView(viewToRemove);
                }
            }

            if (mapView != null) {
                mapView.onDestroy();
            }

            googleMap = null;
            mapView = null;
            mapContainer = null;
            currentLocationButton = null;

            call.resolve();
        });
    }

    @PluginMethod
    public void enableMapInteraction(PluginCall call) {
        if (mapView == null) {
            call.reject("Map not initialized");
            return;
        }

        getActivity().runOnUiThread(() -> {
            // Bring map to front when interaction is needed
            if (mapContainer != null) {
                mapContainer.bringToFront();
                // Set elevation to ensure it's above other views
                mapContainer.setElevation(10f);
                // Reset WebView elevation
                getBridge().getWebView().setElevation(0f);
            }

            // Add a small delay to prevent immediate re-triggering
            mapContainer.postDelayed(() -> {

            }, 50);
            call.resolve();
        });
    }

    @PluginMethod
    public void disableMapInteraction(PluginCall call) {
        if (mapView == null) {
            call.reject("Map not initialized");
            return;
        }

        getActivity().runOnUiThread(() -> {
            // Reset elevation and bring WebView to front
            if (mapContainer != null) {
                mapContainer.setElevation(-40f);
            }
            getBridge().getWebView().bringToFront();
            // Set WebView elevation higher to ensure it's on top
            getBridge().getWebView().setElevation(20f);

            call.resolve();
        });
    }

    private void setupMapListeners() {
        googleMap.setOnMarkerClickListener(marker -> {
            LatLng position = marker.getPosition();

            Projection projection = googleMap.getProjection();
            Point screenPoint = projection.toScreenLocation(position);

            int[] mapLocation = new int[2];
            mapView.getLocationOnScreen(mapLocation);

            int[] containerLocation = new int[2];
            mapContainer.getLocationOnScreen(containerLocation);

            float density = getActivity().getResources().getDisplayMetrics().density;

            int mapPaddingLeft = mapView.getPaddingLeft();
            int mapPaddingTop = mapView.getPaddingTop();

            int absoluteX = (int) ((mapLocation[0] + screenPoint.x + mapPaddingLeft) / density);
            int absoluteY = (int) ((mapLocation[1] + screenPoint.y + mapPaddingTop) / density);

            int webViewX = (int) ((containerLocation[0] + mapX + screenPoint.x) / density);
            int webViewY = (int) ((containerLocation[1] + mapY + screenPoint.y) / density);

            JSObject data = new JSObject();
            data.put("mapId", mapId);
            data.put("latitude", marker.getPosition().latitude);
            data.put("longitude", marker.getPosition().longitude);
            data.put("title", marker.getTitle());
            data.put("markerId", marker.getId());

            data.put("screenX", webViewX);
            data.put("screenY", webViewY);

            data.put("mapX", screenPoint.x);
            data.put("mapY", screenPoint.y);

            data.put("debug", new JSObject() {{
                put("mapLocationX", mapLocation[0]);
                put("mapLocationY", mapLocation[1]);
                put("containerLocationX", containerLocation[0]);
                put("containerLocationY", containerLocation[1]);
                put("density", density);
                put("mapX", absoluteX);
                put("mapY", absoluteY);
                put("screenPointX", screenPoint.x);
                put("screenPointY", screenPoint.y);
            }});

            notifyListeners("onMarkerClick", data);
            return true;
        });

        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                JSObject data = new JSObject();
                data.put("mapId", mapId);
                data.put("markerId", marker.getId());
                data.put("latitude", marker.getPosition().latitude);
                data.put("longitude", marker.getPosition().longitude);
                data.put("title", marker.getTitle());
                notifyListeners("onMarkerDragStart", data);
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                JSObject data = new JSObject();
                data.put("mapId", mapId);
                data.put("markerId", marker.getId());
                data.put("latitude", marker.getPosition().latitude);
                data.put("longitude", marker.getPosition().longitude);
                data.put("title", marker.getTitle());
                notifyListeners("onMarkerDrag", data);
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                JSObject data = new JSObject();
                double lat = marker.getPosition().latitude;
                double lng = marker.getPosition().longitude;

                data.put("mapId", mapId);
                data.put("markerId", marker.getId());
                data.put("latitude", lat);
                data.put("longitude", lng);
                data.put("title", marker.getTitle());

                // Reverse geocoding
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        data.put("address", address.getAddressLine(0));
                    }
                } catch (IOException e) {
                    Log.e(MAPS_TAG, "Geocoder error", e);
                }

                notifyListeners("onMarkerDragEnd", data);
            }
        });

        // Map click listener
        googleMap.setOnMapClickListener(latLng -> {
            JSObject data = new JSObject();
            data.put("mapId", mapId);
            data.put("latitude", latLng.latitude);
            data.put("longitude", latLng.longitude);
            notifyListeners("onMapClick", data);
        });

        googleMap.setOnCameraMoveListener(() -> {
            LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
            JSObject data = new JSObject();

            data.put("north", bounds.northeast.latitude);
            data.put("south", bounds.southwest.latitude);
            data.put("east", bounds.northeast.longitude);
            data.put("west", bounds.southwest.longitude);
            data.put("center_lat", bounds.getCenter().latitude);
            data.put("center_lng", bounds.getCenter().longitude);
            notifyListeners("onBoundsChanged", data);
        });
    }

    @PluginMethod
    public void clearMarkers(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (googleMap == null) {
                call.reject("Map not ready");
                return;
            }

            try {
                // Remove all markers from the map
                for (Marker marker : markers) {
                    if (marker != null) {
                        marker.remove();
                    }
                }

                // Clear the markers list
                markers.clear();

                JSObject result = new JSObject();
                result.put("cleared", true);
                result.put("message", "All markers cleared successfully");
                call.resolve(result);

            } catch (Exception e) {
                Log.e(MAPS_TAG, "Error clearing markers: " + e.getMessage());
                call.reject("Failed to clear markers: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void addMarker(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (googleMap == null) {
                call.reject("Map not ready");
                return;
            }

            double lat = call.getDouble("latitude", 0.0);
            double lng = call.getDouble("longitude", 0.0);
            String title = call.getString("title", "");
            boolean draggable = call.getBoolean("draggable", false);

            LatLng position = new LatLng(lat, lng);
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .draggable(draggable)
            );

            if (marker != null) {
                markers.add(marker);
            }

            JSObject result = new JSObject();
            result.put("markerId", marker.getId());
            call.resolve(result);
        });
    }

    @PluginMethod
    public void addCustomMarker(PluginCall call) throws JSONException {
        getActivity().runOnUiThread(() -> {
            if (googleMap == null) {
                call.reject("Map not ready");
                return;
            }

            JSObject position = call.getObject("position", null);
            String iconImage = call.getString("iconImage", null);

            if (position == null) {
                call.reject("position is required");
                return;
            }

            double lat, lng = 0;
            try {
                lat = position.getDouble("latitude");
                lng = position.getDouble("longitude");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            LatLng latLng = new LatLng(lat, lng);
            Bitmap bitmap = null;

            if (iconImage != null && iconImage.startsWith("data:image")) {
                // Handle base64 image
                String base64Data = iconImage.substring(iconImage.indexOf(",") + 1);
                byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap original = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                float scaleFactor = 1.5f;
                int newWidth = (int)(original.getWidth() * scaleFactor);
                int newHeight = (int)(original.getHeight() * scaleFactor);
                bitmap = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
            } else {
                try {
                    JSArray colorArray = call.getArray("colors");
                    String icon = call.getString("mdiIcon", "default");

                    String color1 = colorArray.getString(0);
                    String color2 = colorArray.getString(1);
                    String color3 = colorArray.getString(2);

                    bitmap = generateMarkerBitmap(color1, color2, color3, icon);
                } catch (JSONException e) {
                    call.reject("Invalid color array");
                    return;
                }
            }

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            );

            if (marker != null) {
                markers.add(marker);
            }

            JSObject result = new JSObject();
            result.put("markerId", marker.getId());
            call.resolve(result);
        });
    }

    @PluginMethod
    public void moveCamera(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (googleMap == null) {
                call.reject("Map not ready");
                return;
            }

            double lat = call.getDouble("latitude", 0.0);
            double lng = call.getDouble("longitude", 0.0);
            float zoom = call.getFloat("zoom", 14.0F);

            LatLng position = new LatLng(lat, lng);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
            call.resolve();
        });
    }

    @PluginMethod
    public void setZoomLimits(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (googleMap == null) {
                call.reject("Map not ready");
                return;
            }

            Float minZoom = call.getFloat("minZoom");
            Float maxZoom = call.getFloat("maxZoom");

            try {
                UiSettings uiSettings = googleMap.getUiSettings();
                if (minZoom != null) {
                    googleMap.setMinZoomPreference(minZoom);
                }
                if (maxZoom != null) {
                    googleMap.setMaxZoomPreference(maxZoom);
                }
                call.resolve();
            } catch (Exception e) {
                call.reject("Failed to set zoom limits: " + e.getMessage());
            }
        });
    }

    // Helper method to get scaled pixels (similar to Kotlin version)
    private int getScaledPixels(Bridge bridge, int pixels) {
        float scale = bridge.getActivity().getResources().getDisplayMetrics().density;
        return (int) (pixels * scale + 0.5f);
    }

    @Override
    protected void handleOnPause() {
        super.handleOnPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void handleOnResume() {
        super.handleOnResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    private Bitmap generateMarkerBitmap(String color1, String color2, String color3, String icon) {
        int width = 100;
        int height = 125;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        int circleRadius = 50;
        int centerX = width / 2;
        int centerY = width / 2;

        int triangleStartY = centerY + (int)(circleRadius * 0.77);

        paint.setColor(Color.parseColor(color1));
        canvas.drawCircle(centerX, centerY, circleRadius, paint);

        paint.setColor(Color.parseColor(color2));
        canvas.drawCircle(centerX, centerY, 36, paint);

        // Triangle (pin tip)
        Path triangle = new Path();
        triangle.moveTo(centerX, height);
        triangle.lineTo(centerX - 30, triangleStartY);
        triangle.lineTo(centerX + 30, triangleStartY);
        triangle.close();
        paint.setColor(Color.parseColor(color1));
        canvas.drawPath(triangle, paint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor(color3));
        textPaint.setTextSize(54);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(mdiTypeface);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float x = centerX;
        float y = centerY + (textHeight / 2f) - fm.descent;

        canvas.drawText(icon, x, y, textPaint);

        return bitmap;
    }
}