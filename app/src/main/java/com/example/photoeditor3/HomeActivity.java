package com.example.photoeditor3;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.MobileAds;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.RecyclerView;

public class HomeActivity extends BaseActivity implements View.OnClickListener {

    private static final int REQUEST_IMPORT_PHOTO = 1;
    private static final int REQUEST_CROP_PHOTO = 2;
    private static final int REQUEST_TAKE_CAMERA_PHOTO = 112;
    private static final int CAMERA_REQUEST = 101;

    //    ImageButton importBtn;
    private LinearLayout importBtn, cameraBtn, myPicturesBtn, shareItBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        MobileAds.initialize(this, getResources().getString(R.string.app_id));

        checkAndRequestPermissions();

        initUi();

        registerListeners();

        bannerAd();
        reqNewInterstitial(this);


    }

    private void registerListeners() {

        importBtn.setOnClickListener(this);
        cameraBtn.setOnClickListener(this);
        myPicturesBtn.setOnClickListener(this);
        shareItBtn.setOnClickListener(this);
    }

    private void initUi() {

        importBtn = findViewById(R.id.import_image_btn);

        cameraBtn = findViewById(R.id.camera_btn);

        myPicturesBtn = findViewById(R.id.my_photos);

        shareItBtn = findViewById(R.id.share_it);

        adView = findViewById(R.id.home_banner);

    }

    private void importImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMPORT_PHOTO);

    }


    public static Uri bitmapToUriConverter(Context context, Bitmap mBitmap) {
        Uri uri = null;
        try {
            File file = new File(context.getFilesDir(), "Image"
                    + new Random().nextInt() + ".jpeg");
            FileOutputStream out = context.openFileOutput(file.getName(),
                    Context.MODE_PRIVATE);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            //get absolute path
            String realPath = file.getAbsolutePath();
            File f = new File(realPath);
            uri = Uri.fromFile(f);
        } catch (Exception e) {
            Log.e("Your Error Message", e.getMessage());
        }
        return uri;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
//            Bitmap photo = (Bitmap) data.getExtras().get("data");

            cropImage(file);

        } else if (REQUEST_IMPORT_PHOTO == requestCode) {
            if (data != null) {
                final Uri uri = data.getData();

                cropImage(uri);

            }

        } else if (requestCode == REQUEST_TAKE_CAMERA_PHOTO && resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                // only for gingerbread and newer versions

                cropImage(file);

            } else {
                Uri uri = data.getData();

                cropImage(uri);

            }
        }


        /**
         * For cropping gallery image and starting next activity
         */

        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
//                    Uri uri = getImageUri(this, bitmap);
                    Uri uri = bitmapToUriConverter(this, bitmap);
//                    Toast.makeText(this, "" + resultUri, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ChooseActivity.class);
                    intent.putExtra("uri", uri.toString());

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        intent.putExtra("nougat", "nougat");
                    }
                    startActivity(intent);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    String mCurrentPhotoPath;
    Uri file;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    private boolean checkAndRequestPermissions() {
        int camera = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        int storage = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int loc = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
//        int loc2 = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
        }
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
//        if (loc2 != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
//        }
        if (loc != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }


    private void dispatchTakePictureIntent() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        startActivityForResult(takePictureIntent, REQUEST_TAKE_CAMERA_PHOTO);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                file = FileProvider.getUriForFile(this,
                        "com.example.photoeditor3.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, file);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void share() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My application name");
            String shareMessage = "\nLet me recommend you this application\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "choose one"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    private static File getOutputMediaFile() {
//        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES), "CameraDemo");
//
//        if (!mediaStorageDir.exists()) {
//            if (!mediaStorageDir.mkdirs()) {
//                return null;
//            }
//        }
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        return new File(mediaStorageDir.getPath() + File.separator +
//                "IMG_" + timeStamp + ".jpg");
//    }

    /**
     * refreshes image in the gallery
     *
     * @param imageFile
     * @return
     */
    public Uri addImage(File imageFile) {
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.Images.Media.TITLE, "My image title");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public void cropImage(Uri imageUri) {
        // start picker to get image for cropping and then use the image in cropping activity
//        CropImage.activity()
//                .setGuidelines(CropImageView.Guidelines.ON)
//                .start(this);

//      start cropping activity for pre-acquired image saved on the device
        CropImage.activity(imageUri)
                .start(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.import_image_btn:
                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager
                        .PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Allow storage permission to read photos", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, 3);
                } else {
                    importImage();
                }
                break;
            case R.id.camera_btn:
                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager
                        .PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Allow camera permission to take photos", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, 3);
                } else {
                    dispatchTakePictureIntent();
                }
                break;
            case R.id.my_photos:
                if (interstitialAd.isLoaded() && interstitialAd != null) {
                    interstitialAd.show();
                } else {
                    if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager
                            .PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Allow storage permission to read photos", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, 3);
                    } else {
                        Intent intent = new Intent(HomeActivity.this, MyPictures.class);
                        startActivity(intent);
                    }
                    reqNewInterstitial(this);
                }
                interstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        reqNewInterstitial(HomeActivity.this);
                        if ((ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager
                                .PERMISSION_GRANTED)) {
                            Toast.makeText(HomeActivity.this, "Allow storage permission to read photos", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent, 3);
                        } else {
                            Intent intent = new Intent(HomeActivity.this, MyPictures.class);
                            startActivity(intent);
                        }
                    }
                });


//                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager
//                        .PERMISSION_GRANTED)) {
//                    Toast.makeText(this, "Allow storage permission to read photos", Toast.LENGTH_LONG).show();
//                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                    Uri uri = Uri.fromParts("package", getPackageName(), null);
//                    intent.setData(uri);
//                    startActivityForResult(intent, 3);
//                } else {
//                    Intent intent = new Intent(HomeActivity.this, MyPictures.class);
//                    startActivity(intent);
//                }
                break;
            case R.id.share_it:
                share();
                break;
        }
    }
}
