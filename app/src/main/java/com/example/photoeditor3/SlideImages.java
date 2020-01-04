package com.example.photoeditor3;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

//import com.google.android.gms.ads.AdListener;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;

import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.IOException;

import static com.example.photoeditor3.MyPictures.paths;
//import static com.photo.mixer.app.MyPictures.paths;

public class SlideImages extends BaseActivity implements View.OnClickListener {

    ViewFlipper mviewPagers;
    private GestureDetector gesturesDetectors;
    Bitmap mBitmap;
    Uri uriImAGES;
    private ImageButton backImageBtn, shareImageBtn, deleteImageBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_images);
//        adview();

        initUi();
        bannerAd();

        backImageBtn.setOnClickListener(this);
        shareImageBtn.setOnClickListener(this);
        deleteImageBtn.setOnClickListener(this);

        mviewPagers = findViewById(R.id.viewPager);
        // viewPager.setAdapter(new TouchImageAdapter());
        /*viewPager.setFlipInterval(2500);
        viewPager.startFlipping();*/

        String sExternalStorageDirectoryPathm = Environment
                .getExternalStorageDirectory()
                .getAbsolutePath();

//        try {
////            String targetedPath = sExternalStorageDirectoryPathm + "/Blend Photos and Editor";
////            // Toast.makeText(getApplicationContext(), R.string.app_name, Toast.LENGTH_LONG).show();
////            File targetedDirectories = new File(targetedPath);
////
////            File[] files = targetedDirectories.listFiles();
////
////            for (File mfiles : files) {
////                if (mfiles.exists()) {
////                    Bitmap bitmapImage = BitmapFactory.decodeFile(mfiles.toString());
//            ImageView mimageView = new ImageView(this);
//            mimageView.setImageBitmap(bitmapImage);
//            mviewPagers.addView(mimageView);
////                } else {
////                    Toast.makeText(getApplicationContext(), "No Files Found", Toast.LENGTH_SHORT).show();
////                }
////
////            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        // Set in/out flipping animations
/*        viewPager.setInAnimation(this, android.R.anim.fade_in);
        viewPager.setOutAnimation(this, android.R.anim.fade_out);*/

        try {
            String ImagesUris = paths.toString();
            uriImAGES = Uri.fromFile(new File(ImagesUris));
            Bitmap fgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uriImAGES);
            ImageView mimageView = new ImageView(SlideImages.this);
            mimageView.setImageBitmap(fgBitmap);
            mviewPagers.addView(mimageView);

        } catch (IOException e) {
            e.printStackTrace();
        }

        CustomGestureDetector customGestureDetector = new CustomGestureDetector();
        gesturesDetectors = new GestureDetector(this, customGestureDetector);

    }

    private void initUi() {

        backImageBtn = findViewById(R.id.slide_back_img_btn);
        shareImageBtn = findViewById(R.id.slide_share_img_btn);
        deleteImageBtn = findViewById(R.id.slide_delete_img_btn);

        adView = findViewById(R.id.slide_large_banner);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share_images, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.slide_share_img_btn) {
            SharemyPhoto();
            //do something
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gesturesDetectors.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class CustomGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Swipe left (next)
            if (e1.getX() > e2.getX()) {
                mviewPagers.showNext();
            }

            // Swipe right (previous)
            if (e1.getX() < e2.getX()) {
                mviewPagers.showPrevious();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }


//    public void adview() {
//        adView = findViewById(R.id.slides_adViews);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        adView.loadAd(adRequest);
//        adView.setAdListener(new AdListener() {
//
//            @Override
//            public void onAdLoaded() {
//                adView.setVisibility(View.VISIBLE);
//            }
//
//            @Override
//            public void onAdFailedToLoad(int error) {
//                adView.setVisibility(View.GONE);
//            }
//
//        });
//
//
//    }

    private Bitmap getScreenShot(View vi) {
//        _fbMenu.close(true);
//        _fbMenu.setVisibility(View.GONE);
        vi.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(vi.getDrawingCache());
        vi.setDrawingCacheEnabled(false);
        return bitmap;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void SharemyPhoto() {
        try {
            File dir = new File(paths.toString());
            Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.example.photoeditor3.fileprovider", dir);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_SUBJECT, "");
            intent.putExtra(Intent.EXTRA_TEXT, "");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "Share via"));
        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.slide_back_img_btn) {
            onBackPressed();
        } else if (v.getId() == R.id.slide_share_img_btn) {
            SharemyPhoto();
        } else if (v.getId() == R.id.slide_delete_img_btn) {
            deleteFile();
        }
    }

    void deleteFile() {
        File file = new File(paths.toString());

        file.delete();
        callBroadCast();
        Toast.makeText(getApplicationContext(), "Image Deleted Successfully", Toast.LENGTH_SHORT).show();
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }


    public void callBroadCast() {
        if (Build.VERSION.SDK_INT >= 14) {
            Log.e("-->", " >= 14");
            MediaScannerConnection.scanFile(SlideImages.this, new String[]{Environment.getExternalStorageDirectory().toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                /*
                 *   (non-Javadoc)
                 * @see android.media.MediaScannerConnection.OnScanCompletedListener#onScanCompleted(java.lang.String, android.net.Uri)
                 */
                public void onScanCompleted(String path, Uri uri) {
                    Log.e("ExternalStorage", "Scanned " + path + ":");
                    Log.e("ExternalStorage", "-> uri=" + uri);
                }
            });
        } else {
            Log.e("-->", " < 14");
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }
}