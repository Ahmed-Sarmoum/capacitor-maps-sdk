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
    private ViewGroup mapContainer = null;

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

        // Optionally store the API key in metadata if needed (some SDK components might still check there)
        ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                context.getPackageName(),
                PackageManager.GET_META_DATA
        );
        Bundle bundle = appInfo.metaData;
        bundle.putString("com.google.android.geo.API_KEY", apiKey);

        getActivity().runOnUiThread(() -> {
            if (mapView == null) {
                mapView = new MapView(context);
                mapView.onCreate(null);
                mapView.onResume();

                String containerId = call.getString("containerId", "map-container");
                ViewGroup webViewParent = (ViewGroup) bridge.getWebView().getParent();
                View container = getActivity().findViewById(
                        context.getResources().getIdentifier(containerId, "id", context.getPackageName())
                );

                if (container instanceof ViewGroup) {
                    mapContainer = (ViewGroup) container;
                    mapContainer.removeAllViews();
                    mapContainer.addView(mapView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                } else if (webViewParent != null) {
                    // fallback to adding to WebView's parent
                    mapContainer = webViewParent;
                    mapContainer.addView(mapView);
                }

                mapView.getMapAsync(gMap -> {
                    googleMap = gMap;

                    // Marker click listener
                    googleMap.setOnMarkerClickListener(marker -> {
                        JSObject data = new JSObject();
                        data.put("latitude", marker.getPosition().latitude);
                        data.put("longitude", marker.getPosition().longitude);
                        data.put("title", marker.getTitle());
                        notifyListeners("onMarkerClick", data);
                        return false;
                    });

                    // Marker drag listener
                    googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                        @Override
                        public void onMarkerDragStart(Marker marker) {}

                        @Override
                        public void onMarkerDrag(Marker marker) {}

                        @Override
                        public void onMarkerDragEnd(Marker marker) {
                            JSObject data = new JSObject();
                            double lat = marker.getPosition().latitude;
                            double lng = marker.getPosition().longitude;

                            data.put("latitude", lat);
                            data.put("longitude", lng);

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

                    call.resolve();
                });
            } else {
                // Map already initialized, make sure it's visible
                if (mapView.getVisibility() != View.VISIBLE) {
                    mapView.setVisibility(View.VISIBLE);
                }
                call.resolve();
            }
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
            call.resolve();
        });
    }

    @PluginMethod
    public void destroyMap(PluginCall call) {
        if (mapView == null) {
            call.resolve();
            return;
        }

        getActivity().runOnUiThread(() -> {
            if (mapContainer != null) {
                mapContainer.removeView(mapView);
            }
            
            mapView.onDestroy();
            googleMap = null;
            mapView = null;
            
            call.resolve();
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
            googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .draggable(draggable)
            );
            call.resolve();
        });
    }


    @PluginMethod
    public void addCustomMarker(PluginCall call) throws JSONException {
        if (googleMap == null) {
            call.reject("Map not ready");
            return;
        }

        JSObject position = call.getObject("position", null);

        String iconImage = call.getString("iconImage", null); // Base64 image from JS


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
                // Strip base64 header
                String base64Data = iconImage.substring(iconImage.indexOf(",") + 1);
                byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap original = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                // SCALE the image to make it bigger (e.g., 1.5x)
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
                }
            }

            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            );

            call.resolve();
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