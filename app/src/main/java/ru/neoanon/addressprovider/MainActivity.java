package ru.neoanon.addressprovider;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yandex.mapkit.MapKitFactory;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private final String MAPKIT_API_KEY = "YOUR_MAPKIT_API_KEY_HERE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(this);

        MapFragment mapFragment = new MapFragment();

        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        fTrans.replace(R.id.content_frame, mapFragment);
        fTrans.commit();
    }
}
