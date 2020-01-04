package com.example.photoeditor3.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import androidx.exifinterface.media.ExifInterface;

import com.example.photoeditor3.DemoView;
import com.example.photoeditor3.R;

public class ImageUtils {

    public Bitmap setOrientation(Uri uri, Bitmap bitmap, Context context) {
        ExifInterface exif;
        Bitmap scaledBitmap = null;
        try {
            exif = new ExifInterface(getRealPathFromURI(uri.toString(), context));
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scaledBitmap;
    }

    private String getRealPathFromURI(String contentURI, Context context) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    private static Bitmap scaledBitmap(Bitmap formatedBm, int width, int height) {
        int w = formatedBm.getWidth();
        int h = formatedBm.getHeight();
        Matrix matrix = new Matrix();
        float scalew = (float) width / w;
        float scaleh = (float) height / h;
        float scale = scalew < scaleh ? scalew : scaleh;
        matrix.postScale(scale, scale);
        Bitmap bmp = Bitmap.createBitmap(formatedBm, 0, 0, w, h, matrix, true);
        if (!formatedBm.equals(bmp) && !formatedBm.isRecycled()) {
            formatedBm.recycle();
        }

        return bmp;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getRealPathFromUR(Context context, Uri uri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);
        String id = wholeID.split(":")[1];

        String[] column = {MediaStore.Images.Media.DATA};

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);
        int columnIndex = cursor.getColumnIndex(column[0]);
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

//    private String getRealPathFromURI(String contentURI) {
//
//        Uri contentUri = Uri.parse(contentURI);
//        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
//        if (cursor == null) {
//            return contentUri.getPath();
//        } else {
//            cursor.moveToFirst();
//            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
//            return cursor.getString(index);
//        }
//    }
//
//        public Bitmap setOrientation(Uri uri, Bitmap bitmap) {
//        ExifInterface exif;
//        Bitmap scaledBitmap = null;
//        Context context = this;
//        String version = getIntent().getStringExtra("nougat");
//        if (version == null) {
//            version = "";
//        }
//
//        try {
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && version.equals("nougat")) {
//                exif = new ExifInterface(getRealPathFromUR(context, uri));
//            } else {
//                exif = new ExifInterface(getRealPathFromURI(uri.toString()));
//            }
////            exif = new ExifInterface(uri.getPath());
//            int orientation = exif.getAttributeInt(
//                    ExifInterface.TAG_ORIENTATION, 0);
//            Log.d("EXIF", "Exif: " + orientation);
//            Matrix matrix = new Matrix();
//            if (orientation == 6) {
//                matrix.postRotate(90);
//                Log.d("EXIF", "Exif: " + orientation);
//            } else if (orientation == 3) {
//                matrix.postRotate(180);
//                Log.d("EXIF", "Exif: " + orientation);
//            } else if (orientation == 8) {
//                matrix.postRotate(270);
//                Log.d("EXIF", "Exif: " + orientation);
//            }
//            scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0,
//                    bitmap.getWidth(), bitmap.getHeight(), matrix,
//                    true);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return scaledBitmap;
//    }

//    private void takeScreenshot() {
//        Date now = new Date();
//        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
//
//        try {
//            // image naming and path  to include sd card  appending name you choose for file
//            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";
//
//            // create bitmap screen capture
//            DemoView v1 = findViewById(R.id.render_view);
//
//
//            v1.setDrawingCacheEnabled(true);
//            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
//            v1.setDrawingCacheEnabled(false);
//
//            File imageFile = new File(mPath);
//
//            FileOutputStream outputStream = new FileOutputStream(imageFile);
//            int quality = 100;
//            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
//            outputStream.flush();
//            outputStream.close();
//
//            openScreenshot(imageFile);
//        } catch (Throwable e) {
//            // Several error may come out with file handling or DOM
//            e.printStackTrace();
//        }
//    }

//    private void openScreenshot(File imageFile) {
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_VIEW);
//        Uri uri = Uri.fromFile(imageFile);
//        intent.setDataAndType(uri, "image/*");
//        startActivity(intent);
//    }


    public static Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        else
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    //    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    @SuppressWarnings( "deprecation" )
//    public static Intent shareImage(Context context, String pathToImage) {
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
//            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//        else
//            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
//
//        shareIntent.setType("image/*");
//
//        // For a file in shared storage.  For data in private storage, use a ContentProvider.
//        Uri uri = Uri.fromFile(context.getFileStreamPath(pathToImage));
//        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
//        return shareIntent;
//    }


}
