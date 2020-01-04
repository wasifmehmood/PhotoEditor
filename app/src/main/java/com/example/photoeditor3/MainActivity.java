package com.example.photoeditor3;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import co.polarr.qrcode.QRUtils;
import co.polarr.renderer.FilterPackageUtil;
import co.polarr.renderer.PolarrRender;
import co.polarr.renderer.PolarrRenderThread;
import co.polarr.renderer.entities.Adjustment;
import co.polarr.renderer.entities.BrushItem;
import co.polarr.renderer.entities.FilterItem;
import co.polarr.renderer.entities.FilterPackage;
import co.polarr.renderer.entities.MagicEraserHistoryItem;
import co.polarr.renderer.entities.MagicEraserPath;
import co.polarr.renderer.utils.QRCodeUtil;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_IMPORT_PHOTO = 1;
    private static final int REQUEST_IMPORT_QR_PHOTO = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int ACTIVITY_RESULT_QR_SCANNER = 3;

    //    private static final int POINT_BRUSH_MOSIC = 1;
//    private static final int POINT_BRUSH_BLUR = 2;
    private static final int POINT_BRUSH_PAINT = 3;
    private static final int POINT_MAGIC_ERASER = 4;

    private static final int TOUCH_FPS = 30;
    private String brushType;
    private int currentPointState;
    private List<PointF> currentPoints;

    private BrushItem paintBrushItem;
    private int paintState = 0; // 0:idle, 1:paint, 3:mageic eraser

    private SeekBar seekbar;
    private TextView labelTv;

    /**
     * Render View
     */
    private DemoView renderView;
    private RelativeLayout renderRl;
    /**
     * adjustment container
     */
    private View sliderCon;
    /**
     * save adjustment values
     */
    private Map<String, Object> localStateMap = new HashMap<>();
    private Map<String, Object> faceStates = new HashMap<>();
    private FilterItem mCurrentFilter;

    private List<FilterItem> mFilters;
    private PolarrRenderThread polarrRenderThread;
    private long lasUpdateTime;
    private Adjustment currentMask;
    //    private BrushItem currentBrushItem;
    private int inputWidth;
    private int inputHeight;
    private List<MagicEraserHistoryItem> historyItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init render view
        renderRl = (RelativeLayout) findViewById(R.id.render_rl);
        renderView = (DemoView) findViewById(R.id.render_view);
        renderView.setAlpha(0);

        sliderCon = findViewById(R.id.slider);
        sliderCon.setVisibility(View.INVISIBLE);

        labelTv = (TextView) findViewById(R.id.label_tv);
        seekbar = findViewById(R.id.seekbar);

        polarrRenderThread = new PolarrRenderThread(getResources());
        polarrRenderThread.start();

        currentPointState = 0;
        currentPoints = new ArrayList<>();
        renderView.setClickable(true);
        renderView.setOnTouchListener(demoViewTouchListener);

        buttonImport = findViewById(R.id.btn_import);


    }

    Button buttonImport;


    private void startBrush(int brushState) {
        currentPointState = brushState;
        currentPoints.clear();
        updateBrush(null);
        if (currentPointState == POINT_BRUSH_PAINT) {
            setBrushPaint(brushType);
        }
    }

    private void endTouch() {
        if (currentPointState == POINT_MAGIC_ERASER) {
            magicEraserPath(null);
            magicErase(currentPoints);
        }

        currentPoints.clear();
        if (currentPointState == POINT_BRUSH_PAINT) {
            if (paintState == 1) {
                paintState = 0;
                paintBrushItem = null;

                List<PointF> points = new ArrayList<>();
                points.addAll(currentPoints);
                currentPoints.clear();
                renderView.brushAddPoints(points);
                renderView.brushFinish();

                // no need call
//                Bitmap brushBm = renderView.getBrushTextureBm();
//                Bitmap downScaledBm = Bitmap.createScaledBitmap(brushBm, brushBm.getWidth() / 2, brushBm.getHeight() / 2, false);
//                Bitmap upScaledBm = Bitmap.createScaledBitmap(downScaledBm, downScaledBm.getWidth() * 2, downScaledBm.getHeight() * 2, false);
//                brushBm.recycle();
//                downScaledBm.recycle();
//                renderView.setBrushTexture(upScaledBm);
            }
        }
    }

    private void stopBrush() {
        currentPointState = 0;
    }

    private void updateBrush(PointF point) {
        switch (currentPointState) {
            case POINT_BRUSH_PAINT:
                if (point != null) {
                    if (paintState == 0) {
                        setBrushPaint(brushType);
                    }
                }
                break;
            case POINT_MAGIC_ERASER:
                magicEraserPath(currentPoints);
                break;
        }
        lazyUpdate(TOUCH_FPS);
    }

    private void lazyUpdate(int fps) {
        long time = System.currentTimeMillis();
        if (time - lasUpdateTime > (1000f / fps)) {
            if (currentPointState == POINT_BRUSH_PAINT) {
                if (paintBrushItem != null) {
                    List<PointF> points = new ArrayList<>();
                    points.addAll(currentPoints);
                    currentPoints.clear();
                    renderView.brushAddPoints(points);
                }
            } else {
                renderView.updateStates(localStateMap);
            }

            lasUpdateTime = System.currentTimeMillis();
        }
    }

    private View.OnTouchListener demoViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (currentPointState <= 0) {
                return false;
            } else {
                PointF touchPoint = new PointF(event.getX() / v.getWidth(), event.getY() / v.getHeight());
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        currentPoints.add(touchPoint);
                        updateBrush(touchPoint);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        endTouch();
                        break;
                }
                return true;
            }
        }
    };

    public void btnClicked(View view) {
        hideAll();
        switch (view.getId()) {
            case R.id.btn_import:
                importImage();
                break;
//            case R.id.tv_desc:
//                importImageDemo();
//                break;
            case R.id.btn_addjustment:
                showList();
                break;
            case R.id.btn_auto: {
                sliderCon.setVisibility(View.VISIBLE);
                final String label = "Auto enhance";
                renderView.autoEnhance(localStateMap, 0.5f);
                labelTv.setText(label);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f;
                        renderView.autoEnhance(localStateMap, adjustmentValue);

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                seekbar.setProgress(50);
            }

            break;
            case R.id.btn_auto_face:
                renderView.autoEnhanceFace0(localStateMap);
                break;
            case R.id.btn_auto_all: {
                sliderCon.setVisibility(View.VISIBLE);
                final String label = "All enhance";
                renderView.autoEnhanceAll(localStateMap, 0.5f);
                labelTv.setText(label);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f;
                        renderView.autoEnhanceAll(localStateMap, adjustmentValue);

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                seekbar.setProgress(50);
            }

            break;
            case R.id.btn_filters:
                showFilters();
                break;
            case R.id.btn_eraser: {
                currentPointState = POINT_MAGIC_ERASER;
            }
            break;
            case R.id.save_image_btn:

                Bitmap bitmap = getBitmapFromView(renderView);

                String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/req_images");
                myDir.mkdirs();
                Random generator = new Random();
                int n = 10000;
                n = generator.nextInt(n);
                String fname = "Image-" + n + ".jpg";
                File file = new File(myDir, fname);
                Log.i("", "" + file);
                if (file.exists())
                    file.delete();
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

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

    @Override
    protected void onDestroy() {
        polarrRenderThread.interrupt();
        super.onDestroy();
    }

    private boolean checkAndRequirePermission(int permissionRequestId) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    permissionRequestId);

            return false;
        }

        return true;
    }

    private void importImage() {
        findViewById(R.id.tv_desc).setVisibility(View.GONE);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMPORT_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (REQUEST_IMPORT_PHOTO == requestCode) {
            if (data != null) {
                final Uri uri = data.getData();
                renderView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Bitmap imageBm = scaledBitmap(decodeBitmapFromUri(MainActivity.this, uri), renderRl.getWidth(), renderRl.getHeight());
//                        final Bitmap imageBm = decodeBitmapFromUri(MainActivity.this, uri);
                        inputWidth = imageBm.getWidth();
                        inputHeight = imageBm.getHeight();
                        new Thread() {
                            @Override
                            public void run() {
                                float perfectSize = 2000f;
                                float minScale = Math.min(perfectSize / imageBm.getWidth(), perfectSize / imageBm.getHeight());
                                minScale = Math.min(minScale, 1f);
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBm, (int) (minScale * imageBm.getWidth()), (int) (minScale * imageBm.getHeight()), true);
                                FaceUtil.InitFaceUtil(MainActivity.this);
                                Map<String, Object> faces = FaceUtil.DetectFace(scaledBitmap);
                                FaceUtil.Release();
                                if (scaledBitmap != imageBm) {
                                    scaledBitmap.recycle();
                                }

                                synchronized (imageBm) {
                                    imageBm.notify();
                                }

                                faceStates = faces;
                                localStateMap.putAll(faceStates);

                                renderView.updateStates(localStateMap);
                            }
                        }.start();
                        synchronized (imageBm) {
                            try {
                                imageBm.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        renderView.importImage(imageBm);
                        renderView.setAlpha(1);

                        updateRenderLayout(imageBm.getWidth(), imageBm.getHeight());
                    }
                }, 1000);
            }
        }
    }

    private void updateRenderLayout(int width, int height) {
        int viewWidth = renderRl.getWidth();
        int viewHeight = renderRl.getHeight();

        float scale = Math.min((float) viewWidth / width, (float) viewHeight / height);
        width *= scale;
        height *= scale;

        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) renderView.getLayoutParams();
        rlp.width = width;
        rlp.height = height;
        renderView.setLayoutParams(rlp);
    }

    private static Bitmap decodeBitmapFromUri(Context context, Uri uri) { //, int viewWidth, int viewHight
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap decodedBm = BitmapFactory.decodeStream(inputStream);
            Bitmap formatedBm = decodedBm.copy(Bitmap.Config.ARGB_8888, false);
            decodedBm.recycle();

            return formatedBm;//scaledBitmap(formatedBm, viewWidth, viewHight);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

//    private void reset() {
//        localMasks.clear();
//        localStateMap.clear();
//        localStateMap.putAll(faceStates);
//        FaceUtil.ResetFaceStates(faceStates);
//
//        renderView.updateStates(localStateMap);
//    }

    @Override
    public void finish() {
        releaseRender();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.finish();
    }

    private void releaseRender() {
        renderView.releaseRender();
    }

    private void magicErase(List<PointF> points) {
        MagicEraserPath path = new MagicEraserPath();
        path.points = new ArrayList<>();
        path.points.addAll(points);
        path.radius = 0.031f * inputWidth;

        renderView.renderMagicEraser(path);
    }

    private void magicEraserPath(List<PointF> points) {
        if (points != null) {
            MagicEraserPath path = new MagicEraserPath();
            path.points = new ArrayList<>();
            path.points.addAll(points);
            path.radius = 0.031f * inputWidth;

            renderView.renderMagicEraserPathOverlay(path);
        } else {
            renderView.renderMagicEraserPathOverlay(null);
        }
    }

    List<Adjustment> localMasks = new ArrayList<>();

    private void setBrushPaint(String paintType) {
        BrushItem brushItem = new BrushItem();

        if (paintType.equals("stroke_5")) {
            brushItem.flow = 0.90f;     // A:0.85 B:0.90 C:0.75
            brushItem.size = 0.40f;     // A:0.25 B:0.40 C:0.80
            brushItem.randomize = 0.8f; // A:0.80 B:0.80 C:0.80
            brushItem.spacing = 0.45f;  // A:0.85 B:0.45 C:0.45
            brushItem.hardness = 1.5f;
        } else if (paintType.equals("mosaic")) {
            brushItem.size = 0.25f;     // A:0.14 B:0.25 C:0.5
            brushItem.spacing = 0.5f;
            brushItem.flow = 1f;
            brushItem.randomize = 0f;
            brushItem.hardness = 0.5f;
        } else if (paintType.equals("blur")) {
            brushItem.size = 0.25f;     // A:0.14 B:0.25 C:0.5
            brushItem.spacing = 0.5f;
            brushItem.flow = 1f;
            brushItem.randomize = 0f;
            brushItem.hardness = 0.5f;

        } else if (paintType.equals("stroke_6")) {
            brushItem.flow = 0.70f;     // A:0.70 B:0.70 C:0.70
            brushItem.size = 0.40f;     // A:0.30 B:0.56 C:0.80
            brushItem.randomize = 0.8f; // A:0.50 B:0.50 C:0.50
            brushItem.spacing = 0.45f;  // A:0.85 B:0.45 C:0.45
            brushItem.hardness = 1f;
        } else {
            brushItem.size = 0.25f;     // A:0.14 B:0.25 C:0.5
            brushItem.spacing = 0.5f;
            brushItem.flow = 1f;
            brushItem.randomize = 0f;
            brushItem.hardness = 0.5f;
        }
        brushItem.texture = paintType; // "stroke_5","stroke_6","mosaic","blur"

        paintState = 1;
        paintBrushItem = brushItem;

        renderView.brushStart(paintBrushItem);
    }

    private void showFilters() {
        if (mFilters == null) {
            List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
            mFilters = new ArrayList<>();
            for (FilterPackage filterPackage : packages) {
                mFilters.addAll(filterPackage.filters);
            }
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final CharSequence items[] = new CharSequence[mFilters.size()];
        for (int i = 0; i < mFilters.size(); i++) {
            items[i] = mFilters.get(i).filterName("zh");
        }
        adb.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int n) {
                sliderCon.setVisibility(View.VISIBLE);
                FilterItem filterItem = mFilters.get(n);
                mCurrentFilter = filterItem;

                localStateMap.clear();
                localStateMap.putAll(faceStates);

                renderView.updateStates(mCurrentFilter.state);

                final String label = "filter:" + filterItem.filterName("zh");
                labelTv.setText(label);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f;

                        if (mCurrentFilter != null) {
                            Map<String, Object> interpolateStates = FilterPackageUtil.GetFilterStates(mCurrentFilter, adjustmentValue);

                            localStateMap.clear();
                            localStateMap.putAll(faceStates);
                            localStateMap.putAll(interpolateStates);

                            renderView.updateStates(interpolateStates);

                        }
                        Toast.makeText(MainActivity.this, "FILTERS", Toast.LENGTH_SHORT).show();

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                seekbar.setProgress(50);

                dialog.dismiss();
            }

        });
        adb.setNegativeButton("Cancel", null);
        adb.setTitle("Choose a filter:");
        adb.show();
    }

    private void showList() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final CharSequence items[] = new CharSequence[]{
                "exposure",
                "contrast",
                "saturation",
                "distortion_horizontal",
                "distortion_vertical",
                "fringing",
                "color_denoise",
                "luminance_denoise",
                "dehaze",
                "diffuse",
                "temperature",
                "tint",
                "gamma",
                "highlights",
                "shadows",
                "whites",
                "blacks",
                "clarity",
                "vibrance",
                "highlights_hue",
                "highlights_saturation",
                "shadows_hue",
                "shadows_saturation",
                "balance",
                "sharpen",
                "hue_red",
                "hue_orange",
                "hue_yellow",
                "hue_green",
                "hue_aqua",
                "hue_blue",
                "hue_purple",
                "hue_magenta",
                "saturation_red",
                "saturation_orange",
                "saturation_yellow",
                "saturation_green",
                "saturation_aqua",
                "saturation_blue",
                "saturation_purple",
                "saturation_magenta",
                "luminance_red",
                "luminance_orange",
                "luminance_yellow",
                "luminance_green",
                "luminance_aqua",
                "luminance_blue",
                "luminance_purple",
                "luminance_magenta",
                "grain_amount",
                "grain_size",
                "mosaic_square",
                "mosaic_hexagon",
                "mosaic_dot",
                "mosaic_triangle",
                "mosaic_diamond",
        };

        adb.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int n) {
                sliderCon.setVisibility(View.VISIBLE);
                CharSequence label = items[n];
                if (label.toString().startsWith("mosaic_")) {
                    String type = label.toString().substring("mosaic_".length());
                    localStateMap.put("mosaic_pattern", type);
                    renderView.updateStates(localStateMap);

                    label = "mosaic_size";
                    Toast.makeText(MainActivity.this, "Mosaic type: " + type + ", try to adjust 'mosaic_size'", Toast.LENGTH_LONG).show();
                }

                labelTv.setText(label);
                final CharSequence finalLabel = label;
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f * 2f - 1f;
                        localStateMap.put(finalLabel.toString(), adjustmentValue);

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", finalLabel, adjustmentValue));

                        renderView.updateStates(localStateMap);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                if (localStateMap.containsKey(label.toString())) {
                    float adjustmentValue = (float) localStateMap.get(label.toString());
                    seekbar.setProgress((int) ((adjustmentValue + 1) / 2 * 100));
                    labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                } else {
                    seekbar.setProgress(50);
                }

                dialog.dismiss();
            }

        });
        adb.setNegativeButton("Cancel", null);
        adb.setTitle("Choose a type:");
        adb.show();
    }

    private void hideList() {
    }

    private void hideAll() {
        renderView.setPaintMode(false);
        currentMask = null;
        findViewById(R.id.tv_desc).setVisibility(View.GONE);
        stopBrush();
        sliderCon.setVisibility(View.INVISIBLE);

        hideList();
    }


}
