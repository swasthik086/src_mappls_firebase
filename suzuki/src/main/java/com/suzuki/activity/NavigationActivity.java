package com.suzuki.activity;

import static com.suzuki.activity.RouteNearByActivity.tripID;
import static com.suzuki.fragment.NavigationFragment.navigationModeEnabled;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.Style;
import com.mappls.sdk.maps.annotations.Marker;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.maps.location.LocationComponent;
import com.mappls.sdk.maps.location.LocationComponentActivationOptions;
import com.mappls.sdk.maps.location.LocationComponentOptions;
import com.mappls.sdk.maps.location.OnCameraTrackingChangedListener;
import com.mappls.sdk.maps.location.modes.CameraMode;
import com.mappls.sdk.maps.location.modes.RenderMode;
import com.mappls.sdk.maps.location.permissions.PermissionsListener;
import com.mappls.sdk.maps.location.permissions.PermissionsManager;
import com.mappls.sdk.navigation.NavLocation;
import com.mappls.sdk.navigation.NavigationFormatter;
import com.mappls.sdk.services.api.OnResponseCallback;
import com.mappls.sdk.services.api.Place;
import com.mappls.sdk.services.api.PlaceResponse;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.reversegeocode.MapplsReverseGeoCode;
import com.mappls.sdk.services.api.reversegeocode.MapplsReverseGeoCodeManager;
import com.suzuki.R;

import com.suzuki.application.SuzukiApplication;
import com.suzuki.base.BaseMapActivity;
import com.suzuki.fragment.MapMainFragment;
import com.suzuki.fragment.NavigationFragment;
import com.suzuki.maps.plugins.BearingIconPlugin;
import com.suzuki.maps.plugins.DirectionPolylinePlugin;
import com.suzuki.maps.plugins.MapEventsPlugin;
import com.suzuki.maps.plugins.RouteArrowPlugin;
import com.suzuki.pojo.RecentTripRealmModule;
import com.suzuki.pojo.ViaPointLocationRealmModel;
import com.suzuki.utils.NavigationCompassEngine;
import com.suzuki.utils.NavigationLocationEngine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import timber.log.Timber;


public class NavigationActivity extends BaseMapActivity implements MapplsMap.InfoWindowAdapter,
        View.OnClickListener, MapplsMap.OnMarkerClickListener, MapplsMap.OnMapLongClickListener,
        FragmentManager.OnBackStackChangedListener,
        OnCameraTrackingChangedListener,
        PermissionsListener {

    public static int DEFAULT_PADDING;
    public static int DEFAULT_BOTTOM_PADDING;
    public MapplsMap mapplsMap;
    boolean isVisible = false;
    Handler backStackHandler = new Handler();
    //    bearing plugin
    BearingIconPlugin _bearingIconPlugin;
    private SuzukiApplication app;
    private Bundle savedInstanceState;
    private NavigationLocationEngine navigationLocationEngine;

    private PermissionsManager permissionsManager;
    //location layer plugin
    private DirectionPolylinePlugin directionPolylinePlugin;
    private RouteArrowPlugin routeArrowPlugin;
    private MapEventsPlugin mapEventsPlugin;
    private boolean firstFix;
    private Fragment currentFragment;
    Runnable backStackRunnable = () -> {
        try {
            onBackStackChangedWithDelay();
        } catch (Exception e) {
            Timber.e(e);
        }
    };
    private FloatingActionButton floatingActionButton;

    @SuppressLint("RestrictedApi")
    private void setupUI() {

        floatingActionButton = findViewById(R.id.move_to_current_location);
        floatingActionButton.setOnClickListener(this);
        floatingActionButton.setVisibility(View.GONE);
    }

    public SuzukiApplication getMyApplication() {
        return ((SuzukiApplication) getApplication());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        try {
            app = getMyApplication();
            DEFAULT_PADDING = (int) getResources().getDimension(R.dimen.default_map_padding);
            DEFAULT_BOTTOM_PADDING = (int) getResources().getDimension(R.dimen.default_map_bottom_padding);

            navigationLocationEngine = new NavigationLocationEngine();
            getSupportFragmentManager().addOnBackStackChangedListener(this);
            setupUI();
        } catch (Exception e) {
            //ignore
        }
        this.navigateTo(new NavigationFragment(), true);


    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onLocationChanged(Location location) {

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onClick(View v) {

    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null;
    }

    @Override
    public void onCameraTrackingDismissed() {

    }


    @Override
    public void onCameraTrackingChanged(int currentMode) {

    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }



    public DirectionPolylinePlugin getDirectionPolylinePlugin() {
        return directionPolylinePlugin;
    }


    public MapView getMapView() {
        return mapView;
    }

    public NavLocation getUserLocation() {
        if (app.getCurrentLocation() != null) {
            NavLocation loc = new NavLocation("router");
            loc.setLatitude(app.getCurrentLocation().getLatitude());
            loc.setLongitude(app.getCurrentLocation().getLongitude());
            return loc;
        } else {
            return null;
        }
    }

    public void startNavigation() {
        onBackPressed();
        navigateTo(new NavigationFragment(), true);

    }

    public float getLocationAccuracy() {
        if (app.getCurrentLocation() != null) {
            return app.getCurrentLocation().getAccuracy();
        } else {
            return 0;
        }
    }

    public void clearPOIs() {
        try {
            if (mapplsMap == null)
                return;
            mapplsMap.removeAnnotations();
            if (directionPolylinePlugin != null)
                directionPolylinePlugin.removeAllData();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng latLng) {
//        Toast.makeText(this, "Long clicked", Toast.LENGTH_SHORT).show();

        return false;
    }

    Fragment fragmentOnTopOfStack() {
        int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
        if (index >= 0) {
            FragmentManager.BackStackEntry backEntry = getSupportFragmentManager().getBackStackEntryAt(index);
            String tag = backEntry.getName();
            return getSupportFragmentManager().findFragmentByTag(tag);
        } else {
            return null;
        }
    }

    Fragment getFragmentOnTopOfBackStack() {
        int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
        if (index >= 0) {
            FragmentManager.BackStackEntry backEntry = getSupportFragmentManager().getBackStackEntryAt(index);
            String tag = backEntry.getName();
            Timber.i(tag, " fragment Tag");
            return getSupportFragmentManager().findFragmentByTag(tag);
        } else {
            return null;
        }
    }

    private void onBackStackChangedWithDelay() {
        currentFragment = getFragmentOnTopOfBackStack();

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {

            finish();
        }
    }


    @Override
    public void onBackStackChanged() {
        backStackHandler.removeCallbacksAndMessages(null);
        backStackHandler.postDelayed(backStackRunnable, 100);
    }


    public void getReverseGeoCode(LatLng latLng) {

     //   showProgress();
        MapplsReverseGeoCode reverseGeoCode = MapplsReverseGeoCode.builder()
                .setLocation(latLng.getLatitude(), latLng.getLongitude())
                .build();

        MapplsReverseGeoCodeManager.newInstance(reverseGeoCode).call(new OnResponseCallback<PlaceResponse>() {
            @Override
            public void onSuccess(PlaceResponse placeResponse) {
                if (placeResponse != null) {
                    List<Place> placesList = placeResponse.getPlaces();
                    Place place = placesList.get(0);

                    ELocation eLocation = new ELocation();
                    eLocation.entryLongitude = latLng.getLongitude();

                    eLocation.longitude = latLng.getLongitude();
                    eLocation.entryLatitude = latLng.getLatitude();
                    eLocation.latitude = latLng.getLatitude();
                    eLocation.placeName = place.getFormattedAddress();

                    eLocation.placeAddress = getString(R.string.point_on_map);
                    if (currentFragment != null) {
                        try {
                            ((MapMainFragment) currentFragment).showInfoOnLongClick(eLocation);
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
               // hideProgress();
            }

            @Override
            public void onError(int i, String s) {
                Toast.makeText(NavigationActivity.this, s, Toast.LENGTH_LONG).show();
               // hideProgress();
            }
        });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onMapError(int i, String s) {

    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(Style style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            LocationComponentOptions options = LocationComponentOptions.builder(this)
                    .trackingGesturesManagement(true)
                    .accuracyColor(ContextCompat.getColor(this, R.color.colorAccent))
                    .build();

            // Get an instance of the component
            LocationComponent locationComponent = mapplsMap.getLocationComponent();
            LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(this, style)
                    .locationComponentOptions(options)
                    .locationEngine(navigationLocationEngine)
                    .build();

            // Activate with options
            locationComponent.activateLocationComponent(activationOptions);

            locationComponent.setCompassEngine(new NavigationCompassEngine());

//            locationComponent.setMaxAnimationFps(15);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);


        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    public MapplsMap getMapboxMap() {
        if (mapplsMap != null)
            return mapplsMap;
        return null;
    }


    public BearingIconPlugin getBearingIconPlugin() {
        return _bearingIconPlugin;
    }

    private void showExitDialog() {
        Dialog dialog = new Dialog(this, R.style.custom_dialog);
        dialog.setContentView(R.layout.custom_dialog);

        TextView tvAlertText = dialog.findViewById(R.id.tvAlertText);
        tvAlertText.setText("Do you want to exit from Navigation?");
        ImageView ivCross = dialog.findViewById(R.id.ivCross);
        ivCross.setOnClickListener(v -> dialog.dismiss());

        ImageView ivCheck = dialog.findViewById(R.id.ivCheck);


        ivCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationModeEnabled = "0";
//                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(NavigationActivity.this);
//                SharedPreferences.Editor editor = preferences.edit();
//                editor.putString("endTime",endTime);
//                editor.apply();
          //      Toast.makeText(app, ""+endTime, Toast.LENGTH_SHORT).show();
                dialog.dismiss();

            }
        });

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            showExitDialog();
//            finish();
        } else {
            super.onBackPressed();
        }

    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {

    }

    @Override
    public void onMapReady(MapplsMap map) {
        try {
            if (map == null)
                return;
            this.mapplsMap = map;
            mapplsMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    try {
//                        TrafficPlugin trafficPlugin = new TrafficPlugin(mapView, mapplsMap);
//                        trafficPlugin.setEnabled(false);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    directionPolylinePlugin = new DirectionPolylinePlugin(mapView, map);
                    routeArrowPlugin = new RouteArrowPlugin(mapView, mapplsMap);
                    mapEventsPlugin = new MapEventsPlugin(mapView, mapplsMap);
                    enableLocationComponent(style);
                    directionPolylinePlugin.setEnableCongestion(true);

                    _bearingIconPlugin = new BearingIconPlugin(mapView, mapplsMap);
                    map.setInfoWindowAdapter(NavigationActivity.this);
                    mapplsMap.setMaxZoomPreference(18.5);
                    mapplsMap.setMinZoomPreference(4);
                    setCompassDrawable();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   public RouteArrowPlugin getRouteArrowPlugin() {
        return routeArrowPlugin;
    }

    public MapEventsPlugin getMapEventPlugin() {
        return mapEventsPlugin;
    }

    public void setCompassDrawable() {
        mapView.getCompassView().setBackgroundResource(R.drawable.compass_background);
        mapplsMap.getUiSettings().setCompassImage(ContextCompat.getDrawable(this, R.drawable.compass_north_up));
        int padding = dpToPx( 8);
        int elevation = dpToPx( 8);
        mapView.getCompassView().setPadding(padding, padding, padding, padding);
        ViewCompat.setElevation(mapView.getCompassView(), elevation);
        mapplsMap.getUiSettings().setCompassMargins(dpToPx(20),dpToPx(120),dpToPx(20),dpToPx(20));
    }

    public  int dpToPx(final float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

}
