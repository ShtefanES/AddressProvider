package ru.neoanon.addressprovider;


import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.GeoObjectCollection;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.ScreenPoint;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateSource;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.SearchType;
import com.yandex.mapkit.search.Session;
import com.yandex.mapkit.search.SuggestItem;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class MapFragment extends Fragment {
    private static final String TAG = MapFragment.class.getSimpleName();
    private Unbinder unbinder;
    private static final int PERMISSIONS_CODE = 109;

    private static final double DESIRED_ACCURACY = 0;
    private static final long MINIMAL_TIME = 0;
    private static final double MINIMAL_DISTANCE = 50;
    private static final boolean USE_IN_BACKGROUND = false;

    private Session.SearchListener mSearchAddressListener;
    private Session.SearchListener mSearchPointListener;
    private LocationListener myLocationListener;
    private SearchManager.SuggestListener suggestListener;
    private CameraListener cameraListener;

    public static final int COMFORTABLE_ZOOM_LEVEL = 18;//если уровень будет ниже, получить улицу с номером дома не получится
    private static final float START_ZOOM_LEVEL = 14.0f;

    private static final Point START_LOCATION = new Point(53.35, 83.76);
    private final double BOX_SIZE = 0.2;
    private final BoundingBox BOUNDING_BOX = new BoundingBox(
            new Point(START_LOCATION.getLatitude() - BOX_SIZE, START_LOCATION.getLongitude() - BOX_SIZE),
            new Point(START_LOCATION.getLatitude() + BOX_SIZE, START_LOCATION.getLongitude() + BOX_SIZE));
    private final int RESULT_NUMBER_LIMIT = 5;
    private final SearchOptions SEARCH_OPTIONS = new SearchOptions().setSearchTypes(
            SearchType.GEO.value);

    private Session searchSession;
    private SearchManager searchManager;

    private boolean isUseSuggestions;

    private QueryHelper queryHelper;
    private RegionHelper regionHelper;
    private Point myLocation;

    private ScreenPoint centerPointOfScreen;

    private LocationManager locationManager;
    private SuggestAdapter suggestAdapter;

    @BindView(R.id.suggest_recycler_view)
    RecyclerView suggestResultView;
    @BindView(R.id.mapview)
    MapView mapView;
    @BindView(R.id.sv_search_address)
    SearchView svSearchAddress;
    @BindView(R.id.root_coordinator)
    CoordinatorLayout rootCoordinatorLayout;
    @BindView(R.id.fab_current_location)
    FloatingActionButton fabCurrentLocation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        queryHelper = new QueryHelper(getString(R.string.region_and_town), getString(R.string.region));
        regionHelper = new RegionHelper();

        mSearchPointListener = new Session.SearchListener() {
            @Override
            public void onSearchResponse(Response response) {
                for (GeoObjectCollection.Item searchResult : response.getCollection().getChildren()) {
                    Point resultLocation = searchResult.getObj().getGeometry().get(0).getPoint();
                    if (resultLocation != null) {
                        moveCamera(resultLocation, COMFORTABLE_ZOOM_LEVEL);
                        svSearchAddress.clearFocus();
                        break;
                    }
                }
            }

            @Override
            public void onSearchError(Error error) {
                Log.w(TAG, "onSearchError ERROR when try search point by address");
                Snackbar.make(rootCoordinatorLayout, R.string.error_cant_get_coordinates, Snackbar.LENGTH_LONG).show();
            }
        };

        mSearchAddressListener = new Session.SearchListener() {
            @Override
            public void onSearchResponse(Response response) {
                for (GeoObjectCollection.Item searchResult : response.getCollection().getChildren()) {
                    GeoObject resultObj = searchResult.getObj();

                    if (resultObj != null && resultObj.getName() != null) {
                        isUseSuggestions = false;
                        svSearchAddress.setQuery(resultObj.getName(), false);
                        if (svSearchAddress.isIconified()) {
                            svSearchAddress.setIconified(false);
                            svSearchAddress.clearFocus();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onSearchError(Error error) {
                Log.w(TAG, "onSearchError ERROR when try search address by point");
                Snackbar.make(rootCoordinatorLayout, R.string.error_cant_get_address, Snackbar.LENGTH_LONG).show();
            }
        };

        myLocationListener = new LocationListener() {
            @Override
            public void onLocationUpdated(Location location) {
                if (myLocation == null) {
                    moveCamera(location.getPosition(), COMFORTABLE_ZOOM_LEVEL);
                }
                myLocation = location.getPosition();
                Log.w(TAG, "my location - " + myLocation.getLatitude() + "," + myLocation.getLongitude());
            }

            @Override
            public void onLocationStatusUpdated(LocationStatus locationStatus) {
                if (locationStatus == LocationStatus.NOT_AVAILABLE) {
                    Snackbar.make(rootCoordinatorLayout, R.string.error_cant_get_my_location, Snackbar.LENGTH_LONG).show();
                }
            }
        };

        suggestListener = new SearchManager.SuggestListener() {
            @Override
            public void onSuggestResponse(List<SuggestItem> list) {
                List<String> suggestResult = new ArrayList<>();
                for (int i = 0; i < Math.min(RESULT_NUMBER_LIMIT, list.size()); i++) {
                    String changedResult = queryHelper.cutResultQuery(list.get(i).getDisplayText());
                    if (changedResult != null) {
                        suggestResult.add(changedResult);
                    }
                }
                if (svSearchAddress.getQuery().length() != 0) {
                    suggestResultView.setVisibility(View.VISIBLE);
                    suggestAdapter.updateData(suggestResult);
                }
            }

            @Override
            public void onSuggestError(Error error) {
                Log.w(TAG, "error on onSuggestResponse");
                Snackbar.make(rootCoordinatorLayout, R.string.error_cant_get_suggestions, Snackbar.LENGTH_LONG).show();
            }
        };

        cameraListener = new CameraListener() {
            @Override
            public void onCameraPositionChanged(Map map, CameraPosition cameraPosition, CameraUpdateSource cameraUpdateSource, boolean b) {
                if (b) {
                    Point centerOfScreen = mapView.screenToWorld(centerPointOfScreen);
                    if (!regionHelper.checkCityContainPoint(centerOfScreen)) {
                        new AlertDialog.Builder(MapFragment.this.getContext())
                                .setMessage(R.string.error_message_going_out_of_the_city)
                                .setPositiveButton(R.string.btn_name_yes,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface arg0, int arg1) {
                                                moveCamera(START_LOCATION, START_ZOOM_LEVEL);
                                            }
                                        })
                                .create()
                                .show();
                        return;
                    }
                    requestAddress(centerOfScreen);
                }
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        unbinder = ButterKnife.bind(this, view);

        searchManager = MapKitFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
        prepareSearchView();

        locationManager = MapKitFactory.getInstance().createLocationManager();

        setupSuggestionRecyclerView();

        setupCenterPointOfScreen();
        mapView.getMap().addCameraListener(cameraListener);

        moveCamera(START_LOCATION, START_ZOOM_LEVEL);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();

        if(!canDetermineLocation()){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_CODE);
        }else {
        subscribeLocationUpdate();
        }

        if (svSearchAddress != null) {
            svSearchAddress.clearFocus();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        locationManager.unsubscribe(myLocationListener);
        MapKitFactory.getInstance().onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    subscribeLocationUpdate();

                } else {
                    new AlertDialog.Builder(this.getContext())
                            .setMessage(R.string.rights_are_required_message)
                            .setPositiveButton(R.string.btn_name_grant_permission,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            if(!canDetermineLocation()){
                                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_CODE);
                                            }
                                        }
                                    })
                            .setCancelable(false)
                            .create()
                            .show();
                }
                return;
            }
        }
    }

    @OnClick(R.id.fab_current_location)
    public void onFabCurrentLocation() {
        if (myLocation == null) {
            Snackbar.make(rootCoordinatorLayout, R.string.coordinates_are_not_yet_determinate, Snackbar.LENGTH_SHORT).show();
            return;
        }

        moveCamera(myLocation, COMFORTABLE_ZOOM_LEVEL);
    }

    private boolean canDetermineLocation() {
        int perm = ActivityCompat.checkSelfPermission(MapFragment.this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGeoPoint(String query) {
        searchSession = searchManager.submit(
                query,
                Geometry.fromPoint(START_LOCATION),
                SEARCH_OPTIONS,
                mSearchPointListener);
    }

    private void requestAddress(Point point) {
        searchSession = searchManager.submit(
                point,
                COMFORTABLE_ZOOM_LEVEL,
                SEARCH_OPTIONS,
                mSearchAddressListener
        );
    }

    private void prepareSearchView() {
        svSearchAddress.setQueryHint(getString(R.string.search_view_hint));
        svSearchAddress.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                suggestResultView.setVisibility(View.GONE);
                fabCurrentLocation.show();
                requestGeoPoint(svSearchAddress.getQuery().toString());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.equals("")) {
                    suggestResultView.setVisibility(View.GONE);
                    fabCurrentLocation.show();
                    isUseSuggestions = true;
                    return false;
                }
                if (isUseSuggestions) {
                    requestSuggest(queryHelper.getQueryWithRegion(newText));
                }
                isUseSuggestions = true;
                return false;
            }
        });
    }

    private void moveCamera(Point point, float zoom) {
        mapView.getMap().move(
                new CameraPosition(point, zoom, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1),
                null);
    }

    private void subscribeLocationUpdate() {
        if (locationManager != null && myLocationListener != null) {
            locationManager.subscribeForLocationUpdates(DESIRED_ACCURACY, MINIMAL_TIME, MINIMAL_DISTANCE, USE_IN_BACKGROUND, myLocationListener);
        }
    }

    private void requestSuggest(String query) {
        fabCurrentLocation.hide();
        searchManager.suggest(query, BOUNDING_BOX, SEARCH_OPTIONS, suggestListener);
    }

    private void setupSuggestionRecyclerView() {
        suggestAdapter = new SuggestAdapter(new SuggestAdapter.OnSuggestClickListener() {
            @Override
            public void onSuggestionClick(String suggestionStr) {
                isUseSuggestions = false;
                svSearchAddress.setQuery(suggestionStr, false);
                suggestResultView.setVisibility(View.GONE);
                fabCurrentLocation.show();
            }
        });
        suggestResultView.setAdapter(suggestAdapter);
    }

    private void setupCenterPointOfScreen() {
        int actionBarHeight = calculateActionBarHeight();
        int mWidth = this.getResources().getDisplayMetrics().widthPixels;
        int mHeight = this.getResources().getDisplayMetrics().heightPixels;
        float x = mWidth / 2.f;
        float y = mHeight / 2.f - actionBarHeight;
        centerPointOfScreen = new ScreenPoint(x, y);
    }

    private int calculateActionBarHeight() {
        TypedValue tv = new TypedValue();
        return (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) ?
                TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics()) : 0;
    }
}
