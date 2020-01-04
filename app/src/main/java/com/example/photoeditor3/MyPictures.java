package com.example.photoeditor3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.shreyaspatil.MaterialDialog.interfaces.DialogInterface;
import com.theartofdev.edmodo.cropper.CropImage;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MyPictures extends BaseActivity implements View.OnClickListener {

    ImageAdapter myImageAdapters;
    ArrayList<String> stringArrayLists = new ArrayList<String>();
    GridView mgridview;
    TextView mtextView1;
    static String path;
    static File paths;
    private ImageButton backImageBtn;
    private FloatingActionButton floatingActionButton;
    private static final int REQUEST_TAKE_CAMERA_PHOTO = 112;
    private static final int CAMERA_REQUEST = 101;
    private static final int REQUEST_IMPORT_PHOTO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_pictures);
        InitView();
        mtextView1 = findViewById(R.id.textView1);
        backImageBtn = findViewById(R.id.my_pictures_back_img_btn);
        floatingActionButton = findViewById(R.id.floating_button);

        backImageBtn.setOnClickListener(this);
        floatingActionButton.setOnClickListener(this);

        try {
            mgridview = (GridView) findViewById(R.id.gridview_itemsGallery);
            myImageAdapters = new ImageAdapter(this);
            mgridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
            mgridview.setMultiChoiceModeListener(new MultiChoiceModeListener());
            mgridview.setAdapter(myImageAdapters);

            String ExternalStorageDirectoryPath = Environment.getExternalStorageDirectory() + "/Photo Editor/";

            // Toast.makeText(getApplicationContext(), R.string.app_name, Toast.LENGTH_LONG).show();
            File targetDirectories = new File(ExternalStorageDirectoryPath);

            File[] files = targetDirectories.listFiles();
            for (File file : files) {
                if (file.exists()) {
                    myImageAdapters.add(file.getAbsolutePath());
                } else {
//                      Toast.makeText(getApplicationContext(), "No File Found", Toast.LENGTH_SHORT).show();
                }
            }
//            gridview listener
            mgridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    path = (String) adapterView.getItemAtPosition(i);
//                    SinglePicture.mpath = path;
//                    paths = path;

//                    Toast.makeText(getApplicationContext(), "" + path, Toast.LENGTH_SHORT).show();
//                    intent.putExtra("idkey", path); // pass the id

                    File pathsa = new File(path);
                    paths = pathsa;
                    Intent intent = new Intent(getApplicationContext(), SlideImages.class);

                    startActivity(intent);
                }
            });

            bannerAd();

        } catch (Exception r) {
            r.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void InitView() {
        mtextView1 = findViewById(R.id.textView1);

        adView = findViewById(R.id.my_pic_banner);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.my_pictures_back_img_btn) {
            onBackPressed();
        } else if (v.getId() == R.id.floating_button) {
            dialog();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {

            cropImage(file);

            //  imageView.setImageBitmap(photo);
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
                    Uri uri = getImageUri(this, bitmap);
//                    Toast.makeText(this, "" + resultUri, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ChooseActivity.class);
                    intent.putExtra("uri", uri.toString());

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        intent.putExtra("nougat", "nougat");
                    }
//                openCropActivity(uri, uri);  // 4
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

    Uri file;

    String mCurrentPhotoPath;

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

    public void cropImage(Uri imageUri) {
        // start picker to get image for cropping and then use the image in cropping activity

//      start cropping activity for pre-acquired image saved on the device
        CropImage.activity(imageUri)
                .start(this);

    }

    private void importImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMPORT_PHOTO);

    }

    BottomSheetMaterialDialog mBottomSheetDialog;

    public void dialog() {
        mBottomSheetDialog = new BottomSheetMaterialDialog.Builder(MyPictures.this)
                .setTitle("Choose One")
                .setCancelable(true)
                .setPositiveButton("Camera", R.drawable.camera, new BottomSheetMaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        Toast.makeText(getApplicationContext(), "Camera!", Toast.LENGTH_SHORT).show();
                        dispatchTakePictureIntent();
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton("Gallery", R.drawable.importphoto, new BottomSheetMaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        Toast.makeText(getApplicationContext(), "Gallery!", Toast.LENGTH_SHORT).show();
                        importImage();
                        dialogInterface.dismiss();
                    }
                })
                .build();

        // Show Dialog
        mBottomSheetDialog.show();
    }

    public class ImageAdapter extends BaseAdapter {

        ArrayList<String> mitemList = new ArrayList<String>();
        private Context mContext;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        void add(String path) {
            mitemList.add(path);

        }

        @Override
        public int getCount() {
            if (mitemList.size() == 0) {
                mtextView1.setVisibility(View.VISIBLE);
            } else {
                mtextView1.setVisibility(View.GONE);
            }
            return mitemList.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return mitemList.get(arg0);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;

            CheckableLayout l;
            if (convertView == null) {  // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(220, 220));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);

                l = new CheckableLayout(mContext);

                l.setLayoutParams(new GridView.LayoutParams(
                        GridView.LayoutParams.WRAP_CONTENT,
                        GridView.LayoutParams.WRAP_CONTENT));
                l.addView(imageView);

            } else {
                l = (CheckableLayout) convertView;
                imageView = (ImageView) l.getChildAt(0);
            }

            Bitmap bm = decodeSampledBitmapFromUri(mitemList.get(position), 220, 220);

            imageView.setImageBitmap(bm);
            return l;
        }

        public Bitmap decodeSampledBitmapFromUri(String path, int reqWidth, int reqHeight) {

            Bitmap bm = null;
            // Offial decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            bm = BitmapFactory.decodeFile(path, options);

            return bm;
        }

        public int calculateInSampleSize(
                BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                if (width > height) {
                    inSampleSize = Math.round((float) height / (float) reqHeight);
                } else {
                    inSampleSize = Math.round((float) width / (float) reqWidth);
                }
            }

            return inSampleSize;
        }

    }

    //TODO: multichoice listener method for grid
    public class MultiChoiceModeListener implements
            AbsListView.MultiChoiceModeListener {
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            mode.setTitle("Select Items");
            mode.setSubtitle("1 item selected");
            mode.getMenuInflater().inflate(R.menu.main_menu, menu);
            mode.getMenu().getItem(2).setEnabled(false);

            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            int id = item.getItemId();
            //sahre images

            if (id == R.id.selectall) {
                try {

                    for (int i = 0; i < mgridview.getAdapter().getCount(); i++) {
                        final int position = i;
                        mgridview.setItemChecked(position, true);
                        //l.setChecked(true);
                    }
                    myImageAdapters.notifyDataSetChanged();
                    item.setEnabled(false);

                    mode.getMenu().getItem(2).setEnabled(true);
                } catch (Exception e) {
                    Log.e("tag", e.getMessage());
                }

                return true;
            } else if (id == R.id.ic_delte) {

                for (String path : stringArrayLists/* List of the files you want to send */) {
                    File file = new File(path);

                    file.delete();
                    callBroadCast();
                    Toast.makeText(getApplicationContext(), "Image Deleted Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(getIntent());
                    overridePendingTransition(0, 0);

                }

                return true;
            } else if (id == R.id.deselectall) {
                try {
                    mgridview.clearChoices();
                    myImageAdapters.notifyDataSetChanged();
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(getIntent());
                    overridePendingTransition(0, 0);
                    //l.setChecked(false);
                    item.setEnabled(false);
                    //item.setChecked(false);
                    mode.getMenu().getItem(3).setEnabled(true);
                } catch (Exception e) {
                    Log.e("tag", e.getMessage());
                }

                return true;
            }
            return true;
        }

        public void callBroadCast() {
            if (Build.VERSION.SDK_INT >= 14) {
                Log.e("-->", " >= 14");
                MediaScannerConnection.scanFile(MyPictures.this, new String[]{Environment.getExternalStorageDirectory().toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
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

        public void onDestroyActionMode(ActionMode mode) {

        }

        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {


            if (checked) {
                stringArrayLists.add(myImageAdapters.mitemList.get(position));
                int selectCount = mgridview.getCheckedItemCount();


                switch (selectCount) {
                    case 1:
                        mode.setSubtitle("1 item selected");
                        //l.setChecked(true);
                        mode.getMenu().getItem(2).setEnabled(true);
                        //mode.getMenu().getItem(2).setChecked(true);
                        break;
                    default:
                        mode.setSubtitle("" + selectCount + " items selected");
                        mode.getMenu().getItem(2).setEnabled(true);
                        //l.setChecked(true);
                        //mode.getMenu().getItem(2).setChecked(true);
                        break;
                }
            } else {
                stringArrayLists.remove(myImageAdapters.mitemList.get(position));

                int selectCount = mgridview.getCheckedItemCount();
                switch (selectCount) {
                    case 1:
                        mode.setSubtitle("1 item selected");
                        //l.setChecked(false);
                        break;
                    default:
                        //l.setChecked(true);
                        mode.setSubtitle("" + selectCount + " items selected");
                        break;
                }
            }
        }
    }

    //TODO: checkable class
    public class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        @SuppressWarnings("deprecation")
        public void setChecked(boolean checked) {
            mChecked = checked;
            try {
                setForeground(checked ? getResources().getDrawable(
                        R.drawable.ic_check_black_4dp) : null);

            } catch (Exception ee) {

            }
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void toggle() {
            setChecked(!mChecked);
        }

    }
}