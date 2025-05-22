package com.ahmed.plugin.mapsdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@CapacitorPlugin(name = "CapacitorMapSdk")
public class CapacitorMapSdkPlugin extends Plugin {

    private static final String MAPS_TAG = "CAPACITOR_MAPS_SDK_TAGS";
    private GoogleMap googleMap;
    private MapView mapView;
    private Typeface mdiTypeface = null;
    private FrameLayout mapContainer = null;
    private String mapId = "default-map";

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

        // Create map container similar to the Kotlin version
        mapContainer = new FrameLayout(bridge.getContext());
        mapContainer.setTag(mapId);

        // Set minimum dimensions
        mapContainer.setMinimumHeight(bridge.getWebView().getHeight());
        mapContainer.setMinimumWidth(bridge.getWebView().getWidth());

        // Create layout params for positioning
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                getScaledPixels(bridge, mapWidth),
                getScaledPixels(bridge, mapHeight)
        );
        layoutParams.leftMargin = getScaledPixels(bridge, mapX);
        layoutParams.topMargin = getScaledPixels(bridge, mapY);

        // Add mapView to container
        mapView.setLayoutParams(layoutParams);
        mapContainer.addView(mapView);

        // Add container to WebView parent
        ViewGroup webViewParent = (ViewGroup) bridge.getWebView().getParent();
        webViewParent.addView(mapContainer);

        // Ensure WebView stays on top for UI elements
        bridge.getWebView().bringToFront();
        bridge.getWebView().setBackgroundColor(Color.TRANSPARENT);
    }

    @PluginMethod
    public void updateMapBounds(PluginCall call) {
        if (mapContainer == null) {
            call.reject("Map not initialized");
            return;
        }

        // Get new bounds
        int x = call.getInt("x", mapX);
        int y = call.getInt("y", mapY);
        int width = call.getInt("width", mapWidth);
        int height = call.getInt("height", mapHeight);

        getActivity().runOnUiThread(() -> {
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
    public void setMapVisibility(PluginCall call) {
        if (mapView == null) {
            call.reject("Map not initialized");
            return;
        }

        boolean visible = call.getBoolean("visible", true);

        getActivity().runOnUiThread(() -> {
            mapView.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (mapContainer != null) {
                mapContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
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

            call.resolve();
        });
    }

    @PluginMethod
    public void bringToFront(PluginCall call) {
        if (mapContainer == null) {
            call.reject("Map not initialized");
            return;
        }

        getActivity().runOnUiThread(() -> {
            mapContainer.bringToFront();
            call.resolve();
        });
    }

    private void setupMapListeners() {
        // Marker click listener
        googleMap.setOnMarkerClickListener(marker -> {
            JSObject data = new JSObject();
            data.put("mapId", mapId);
            data.put("latitude", marker.getPosition().latitude);
            data.put("longitude", marker.getPosition().longitude);
            data.put("title", marker.getTitle());
            data.put("markerId", marker.getId());
            notifyListeners("onMarkerClick", data);
            return false;
        });

        // Marker drag listener
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
    }

    @PluginMethod
    public void addMarker(PluginCall call) {
        if (googleMap == null) {
            call.reject("Map not ready");
            return;
        }

        double lat = call.getDouble("latitude", 0.0);
        double lng = call.getDouble("longitude", 0.0);
        String title = call.getString("title", "");
        boolean draggable = call.getBoolean("draggable", false);

        getActivity().runOnUiThread(() -> {
            LatLng position = new LatLng(lat, lng);
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .draggable(draggable)
            );

            JSObject result = new JSObject();
            result.put("markerId", marker.getId());
            call.resolve(result);
        });
    }

    @PluginMethod
    public void addCustomMarker(PluginCall call) throws JSONException {
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

        double lat = position.getDouble("latitude");
        double lng = position.getDouble("longitude");

        getActivity().runOnUiThread(() -> {
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

            JSObject result = new JSObject();
            result.put("markerId", marker.getId());
            call.resolve(result);
        });
    }

    @PluginMethod
    public void moveCamera(PluginCall call) {
        if (googleMap == null) {
            call.reject("Map not ready");
            return;
        }

        double lat = call.getDouble("latitude", 0.0);
        double lng = call.getDouble("longitude", 0.0);
        float zoom = call.getFloat("zoom", 14.0F);

        getActivity().runOnUiThread(() -> {
            LatLng position = new LatLng(lat, lng);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
            call.resolve();
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