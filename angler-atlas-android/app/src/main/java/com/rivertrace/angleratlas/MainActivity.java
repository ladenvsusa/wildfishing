package com.rivertrace.angleratlas;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** Native Android shell for the Angler Atlas beta. */
public class MainActivity extends Activity {
    private static final int WEB_FILE_CHOOSER = 43;
    private static final int LOCATION_PERMISSION = 55;

    private WebView webView;
    private ValueCallback<Uri[]> pendingFiles;
    private LocationManager locationManager;
    private LocationListener activeLocationListener;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new HydrologyBridge(), "AndroidHydrology");
        webView.addJavascriptInterface(new LocationBridge(), "AndroidLocation");
        webView.addJavascriptInterface(new NavigationBridge(), "AndroidNavigation");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                                       FileChooserParams params) {
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;
                try {
                    startActivityForResult(params.createIntent(), WEB_FILE_CHOOSER);
                    return true;
                } catch (Exception error) {
                    pendingFiles = null;
                    return false;
                }
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WEB_FILE_CHOOSER && pendingFiles != null) {
            pendingFiles.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            pendingFiles = null;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                     int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION) return;
        if (hasLocationPermission()) {
            locateNow();
        } else {
            sendLocationError("定位权限未授权，可在系统设置中开启");
        }
    }

    @Override protected void onDestroy() {
        if (locationManager != null && activeLocationListener != null) {
            try { locationManager.removeUpdates(activeLocationListener); } catch (SecurityException ignored) { }
        }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocation() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);
            return;
        }
        locateNow();
    }

    private void locateNow() {
        if (!hasLocationPermission()) return;
        Location best = null;
        try {
            for (String provider : locationManager.getProviders(true)) {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate == null) continue;
                if (best == null || candidate.getTime() > best.getTime()
                    || candidate.getAccuracy() < best.getAccuracy()) best = candidate;
            }
            if (best != null && System.currentTimeMillis() - best.getTime() < 120_000L) {
                sendLocation(best);
                return;
            }
            String provider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            if (!locationManager.isProviderEnabled(provider)) {
                sendLocationError("系统定位服务未开启");
                return;
            }
            activeLocationListener = location -> {
                try { locationManager.removeUpdates(activeLocationListener); } catch (SecurityException ignored) { }
                activeLocationListener = null;
                sendLocation(location);
            };
            locationManager.requestSingleUpdate(provider, activeLocationListener, Looper.getMainLooper());
            Location fallback = best;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (activeLocationListener == null) return;
                try { locationManager.removeUpdates(activeLocationListener); } catch (SecurityException ignored) { }
                activeLocationListener = null;
                if (fallback != null) sendLocation(fallback);
                else sendLocationError("定位超时，请到开阔位置重试");
            }, 12_000L);
        } catch (SecurityException | IllegalArgumentException error) {
            sendLocationError("定位失败：" + error.getMessage());
        }
    }

    private void sendLocation(Location location) {
        String script = "window.onNativeLocation && window.onNativeLocation("
            + location.getLatitude() + "," + location.getLongitude() + ","
            + location.getAccuracy() + ");";
        webView.evaluateJavascript(script, null);
    }

    private void sendLocationError(String message) {
        String script = "window.onNativeLocationError && window.onNativeLocationError("
            + JSONObject.quote(message) + ");";
        webView.evaluateJavascript(script, null);
    }

    private void openExternal(Uri appUri, Uri webFallback) {
        try {
            Intent appIntent = new Intent(Intent.ACTION_VIEW, appUri);
            appIntent.setPackage("com.autonavi.minimap");
            startActivity(appIntent);
        } catch (ActivityNotFoundException error) {
            startActivity(new Intent(Intent.ACTION_VIEW, webFallback));
        }
    }

    private final class LocationBridge {
        @JavascriptInterface public void request() {
            runOnUiThread(MainActivity.this::requestLocation);
        }
    }

    private final class NavigationBridge {
        @JavascriptInterface public void navigate(double lat, double lng, String name) {
            runOnUiThread(() -> {
                String encoded = Uri.encode(name == null ? "钓点" : name);
                Uri app = Uri.parse("amapuri://route/plan/?sourceApplication=" + Uri.encode("野水簿")
                    + "&dlat=" + lat + "&dlon=" + lng + "&dname=" + encoded + "&dev=0&t=0");
                Uri web = Uri.parse("https://uri.amap.com/navigation?to=" + lng + "," + lat + ","
                    + encoded + "&mode=car&policy=1&src=" + Uri.encode("野水簿"));
                openExternal(app, web);
            });
        }

        @JavascriptInterface public void searchParking(double lat, double lng) {
            runOnUiThread(() -> {
                Uri app = Uri.parse("androidamap://arroundpoi?sourceApplication=" + Uri.encode("野水簿")
                    + "&keywords=" + Uri.encode("停车场") + "&lat=" + lat + "&lon=" + lng + "&dev=0");
                Uri web = Uri.parse("https://uri.amap.com/search?keyword=" + Uri.encode("停车场")
                    + "&center=" + lng + "," + lat + "&src=" + Uri.encode("野水簿"));
                openExternal(app, web);
            });
        }
    }

    private final class HydrologyBridge {
        @JavascriptInterface public void refresh() {
            new CjhHydrologyClient().fetch(new CjhHydrologyClient.Callback() {
                @Override public void onLoaded(List<CjhHydrologyClient.Station> stations) {
                    try {
                        JSONArray rows = new JSONArray();
                        for (CjhHydrologyClient.Station station : stations) {
                            JSONObject row = new JSONObject();
                            row.put("name", station.name);
                            row.put("level", station.level);
                            row.put("trend", station.trendLabel());
                            row.put("measuredAt", station.measuredAt);
                            rows.put(row);
                        }
                        webView.evaluateJavascript("window.applyNativeHydrology && window.applyNativeHydrology("
                            + rows + ");", null);
                    } catch (Exception ignored) { }
                }

                @Override public void onError(String message) {
                    webView.evaluateJavascript("window.onHydrologyFallback && window.onHydrologyFallback();", null);
                }
            });
        }
    }
}
