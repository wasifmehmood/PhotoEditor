package com.example.photoeditor3;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

public class BaseActivity extends AppCompatActivity {



    public AdView adView;
    public InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, getResources().getString(R.string.app_id));


    }

    public void showMessage(Context context, String message){
        Toast.makeText(context, "Testing"+message, Toast.LENGTH_SHORT).show();
    }


    public void reqNewInterstitial(Context context) {
        interstitialAd = new InterstitialAd(context);
        interstitialAd.setAdUnitId(getResources().getString(R.string.Interstitial));
        interstitialAd.loadAd(new AdRequest.Builder().build());
    }

    public void bannerAd() {
        AdRequest adrequest = new AdRequest.Builder()
                .build();
        adView.loadAd(adrequest);
        adView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(int error) {
                adView.setVisibility(View.GONE);
            }

        });
    }
}
