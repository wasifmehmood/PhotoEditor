package com.example.photoeditor3;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.etebarian.meowbottomnavigation.MeowBottomNavigation;
import com.example.photoeditor3.utils.ImageUtils;
import com.google.android.material.snackbar.Snackbar;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.shreyaspatil.MaterialDialog.interfaces.DialogInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.RecyclerView;

import co.polarr.renderer.FilterPackageUtil;
import co.polarr.renderer.PolarrRenderThread;
import co.polarr.renderer.entities.FilterItem;
import co.polarr.renderer.entities.FilterPackage;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ChooseActivity extends BaseActivity implements RecyclerAdapter.RecyclerAdapterOnClickHandler,
        RecyclerAdapter.adapterListener, View.OnClickListener {

    private Uri myUri;

    /**
     * Render View
     */
    private DemoView renderView;
    private RelativeLayout renderRl;

    private static int inputWidth;
    private static int inputHeight;
    /**
     * save adjustment values
     */
    private Map<String, Object> localStateMap = new HashMap<>();
    private Map<String, Object> stateMap = new HashMap<>();
    private Map<String, Object> faceStates = new HashMap<>();
    private RecyclerView mRecyclerView;
    private RecyclerAdapter recyclerAdapter;
    private ArrayList<String> featuresTextViews;
    private ArrayList<Integer> featuresImageViews;
    private ArrayList<String> filterTextViews;
    private ArrayList<Integer> filterImageViews;
    private ArrayList<String> professionalTextViews;
    private ArrayList<Integer> professionalImageViews;

    private PolarrRenderThread polarrRenderThread;

    /**
     * adjustment container
     */
    private View sliderCon;
    private TextView labelTv;
    private SeekBar seekbar;
    private ArrayList<FilterItem> mFiltersList;
    private ImageButton backImageBtn;
    private MeowBottomNavigation bottomNavigation;
    private MeowBottomNavigation bottomNavigationConfirm;
    private LinearLayout linearLayout, bottomLinear;
    private ImageButton closeBtn, checkBtn;

    private ConstraintLayout constraintLayout;

    int modelId = 0;
    int modelConfirmId = 0;

    private Button saveImageButton;
    private ConstraintLayout chooseConstraintLayout;

    void initUi() {

        sliderCon = findViewById(R.id.slider_linear_layout);
        mRecyclerView = findViewById(R.id.recycler);
        bottomNavigation = findViewById(R.id.bottom_navigation);
//        bottomNavigationConfirm = findViewById(R.id.bottom_navigation_confirm);
        renderRl = (RelativeLayout) findViewById(R.id.render_rl);
        renderView = (DemoView) findViewById(R.id.render_view);
        seekbar = findViewById(R.id.seekbar_features);
        labelTv = findViewById(R.id.label_tv_features);
        saveImageButton = findViewById(R.id.save_image_btn);
        linearLayout = findViewById(R.id.save_linear_layout);
        bottomLinear = findViewById(R.id.bottom_linear);
        closeBtn = findViewById(R.id.close_btn);
        checkBtn = findViewById(R.id.check_btn);
        backImageBtn = findViewById(R.id.choose_back_image_btn);

        constraintLayout = findViewById(R.id.const_layout);
        chooseConstraintLayout = findViewById(R.id.choose_constraint_layout);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initUi();

        saveImageButton.setOnClickListener(this);
        backImageBtn.setOnClickListener(this);

        featuresTextViews = new ArrayList<>();
        featuresImageViews = new ArrayList<>();
        filterTextViews = new ArrayList<>();
        filterImageViews = new ArrayList<>();
        professionalTextViews = new ArrayList<>();
        professionalImageViews = new ArrayList<>();
        populateArraylists();
        populateFilterArrayLists();
        populateProfessionalArraylists();

        polarrRenderThread = new PolarrRenderThread(getResources());
        polarrRenderThread.start();

        bottomNavigation.add(new MeowBottomNavigation.Model(1, R.drawable.ic_effect));
        bottomNavigation.add(new MeowBottomNavigation.Model(2, R.drawable.ic_edit_deselected));
        bottomNavigation.add(new MeowBottomNavigation.Model(3, R.drawable.ic_pro_deselected));

//        bottomNavigationConfirm.add(new MeowBottomNavigation.Model(1, R.drawable.ic_close_black_24dp));
//        bottomNavigationConfirm.add(new MeowBottomNavigation.Model(2, R.drawable.ic_check_black_24dp));

        renderView.setAlpha(0);
        renderView.setClickable(true);

        bottomNavigationListener();
        bottomNavigationConfirmListener();


        String uriStr = getIntent().getStringExtra("uri");
        myUri = Uri.parse(uriStr);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgress(0);
        progressDialog.show();

        renderViewThread();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    ProgressDialog progressDialog;

    private void bottomNavigationConfirmListener() {

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                seekbar.setProgress(progressInt);
                labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label2, progress));
//                    labelTv.setText(String.format(Locale.ENGLISH,"%s: %d", label2, (int) ((progress + 1) / 2 * 100)));
//                    renderView.updateStates(stateMap);

                modelConfirmId = 1;

                bottomNavigation.setVisibility(View.VISIBLE);
                bottomLinear.setVisibility(View.INVISIBLE);
                mRecyclerView.setVisibility(View.VISIBLE);
                sliderCon.setVisibility(View.INVISIBLE);

            }
        });

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                modelConfirmId = 2;
                bottomNavigation.setVisibility(View.VISIBLE);
                bottomLinear.setVisibility(View.INVISIBLE);
                mRecyclerView.setVisibility(View.VISIBLE);
                sliderCon.setVisibility(View.INVISIBLE);
            }
        });

    }

    private void bottomNavigationListener() {

        final Context context = this;

        bottomNavigation.setOnShowListener(new Function1<MeowBottomNavigation.Model, Unit>() {
            @Override
            public Unit invoke(MeowBottomNavigation.Model model) {

                if (model.getId() == 1) {

                    constraintLayout.setBackgroundColor(getResources().getColor(R.color.colorGrey));

                    setFilterRecyclerView(filterTextViews, filterImageViews);
                    modelId = 1;

                } else if (model.getId() == 2) {
                    constraintLayout.setBackgroundColor(getResources().getColor(R.color.colorGrey));

                    setRecyclerView(featuresTextViews, featuresImageViews);
                    modelId = 2;

                } else if (model.getId() == 3) {
                    constraintLayout.setBackgroundColor(getResources().getColor(R.color.colorGrey));

                    setProfessionalRecyclerView(professionalTextViews, professionalImageViews);
                    modelId = 3;
                }

                return null;
            }
        });

        bottomNavigation.setOnClickMenuListener(new Function1<MeowBottomNavigation.Model, Unit>() {
            @Override
            public Unit invoke(MeowBottomNavigation.Model model) {

                if (model.getId() == 1) {
                    constraintLayout.setBackgroundColor(getResources().getColor(R.color.colorGrey));

                    setFilterRecyclerView(filterTextViews, filterImageViews);
                    modelId = 1;
                }

                if (model.getId() == 2) {
                    constraintLayout.setBackgroundColor(getResources().getColor(R.color.colorGrey));

                    setRecyclerView(featuresTextViews, featuresImageViews);
                    modelId = 2;
                } else if (model.getId() == 3) {
                    constraintLayout.setBackgroundColor(getResources().getColor(R.color.colorGrey));

                    setProfessionalRecyclerView(professionalTextViews, professionalImageViews);
                    modelId = 3;

                }
                return null;
            }
        });
    }

    /**
     * Convert uri to bitmap and set the orientation of the image to vertical
     *
     * @param context
     * @param uri
     * @return
     */

    private Bitmap decodeBitmapFromUri(Context context, Uri uri) { //, int viewWidth, int viewHeight
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap decodedBm = BitmapFactory.decodeStream(inputStream);
            Bitmap formatedBm = decodedBm.copy(Bitmap.Config.ARGB_8888, false);
            decodedBm.recycle();


//            ImageUtils imageUtils = new ImageUtils();

//            Bitmap orientedBitmap = setOrientation(uri, formatedBm);

            Log.i("aaa", "Uri: " + uri + " Bitmap: " + formatedBm);

//            return orientedBitmap;//scaledBitmap(formatedBm, viewWidth, viewHight);
            return formatedBm;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
//        Toast.makeText(this, "" + width + " H:" + height, Toast.LENGTH_SHORT).show();
        renderView.setLayoutParams(rlp);
    }


    @Override
    public void onClick(String fileName, View view, int position) {


    }

    private FilterItem mCurrentFilter;

    @Override
    public void btnOnClick(View v, int position) {

        if (modelId == 1) {
            bottomNavigation.setVisibility(View.INVISIBLE);
            bottomLinear.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
            sliderCon.setVisibility(View.VISIBLE);

            FilterItem filterItem = mFiltersList.get(position);
            mCurrentFilter = filterItem;

            localStateMap.clear();
            localStateMap.putAll(faceStates);

            renderView.updateStates(mCurrentFilter.state);

            final String label = "filter:" + filterItem.filterName("zh");
            final String subLabel = filterItem.filterName("zh");
            labelTv.setText(subLabel);
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


                    labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", subLabel, adjustmentValue));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            seekbar.setProgress(50);
            float adjustmentValue = (0 - 1) * 2 / 100;
            progressInt = 0;
            progress = (int) adjustmentValue;
            label2 = "filter:" + filterItem.filterName("zh");
            stateMap = mCurrentFilter.state;

        } else if (modelId == 2 || modelId == 3) {
            bottomNavigation.setVisibility(View.INVISIBLE);
            bottomLinear.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
            sliderCon.setVisibility(View.VISIBLE);

//        CharSequence label = featuresTextViews.get(position);
            CharSequence label = v.findViewById(R.id.list_image_view).getTag().toString();

//        if (label.toString().startsWith("mosaic_")) {
//            String type = label.toString().substring("mosaic_".length());
//            localStateMap.put("mosaic_pattern", type);
//            renderView.updateStates(localStateMap);
//
//            label = "mosaic_size";
//            Toast.makeText(ChooseActivity.this, "Mosaic type: " + type + ", try to adjust 'mosaic_size'", Toast.LENGTH_LONG).show();
//        }
//            stateMap = localStateMap;
            String upperString1 = label.toString().substring(0, 1).toUpperCase() + label.toString().substring(1);
            if (upperString1.equals("Luminance_denoise")) {
                labelTv.setText("Smooth");
            } else {
                labelTv.setText(upperString1);
            }
            final CharSequence finalLabel = label;
            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float adjustmentValue = (float) progress / 100f * 2f - 1f;
                    localStateMap.put(finalLabel.toString(), adjustmentValue);

                    String upperString = finalLabel.toString().substring(0, 1).toUpperCase() + finalLabel.toString().substring(1);
                    if (finalLabel.toString().equals("luminance_denoise")) {
                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", "Smooth", adjustmentValue));
                    } else if (finalLabel.toString().equals("clarity")) {
                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", "Structure", adjustmentValue));
                    } else if (finalLabel.toString().equals("exposure")) {
                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", "Brightness", adjustmentValue));

                    } else {
                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", upperString, adjustmentValue));
                    }


                    renderView.updateStates(localStateMap);
//                    Toast.makeText(ChooseActivity.this, "inner", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            if (localStateMap.containsKey(label.toString())) {
                String upperString = label.toString().substring(0, 1).toUpperCase() + label.toString().substring(1);
                float adjustmentValue = (float) localStateMap.get(label.toString());
                seekbar.setProgress((int) ((adjustmentValue + 1) / 2 * 100));
                if (finalLabel.toString().equals("luminance_denoise")) {
                    labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", "Smooth", adjustmentValue));
                } else if (finalLabel.toString().equals("clarity")) {
                    labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", "Structure", adjustmentValue));
                } else if (finalLabel.toString().equals("exposure")) {
                    labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", "Brightness", adjustmentValue));

                } else {
                    labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", upperString, adjustmentValue));
                }

                stateMap = localStateMap;
                progress = adjustmentValue;
                progressInt = (int) ((adjustmentValue + 1) / 2 * 100);
                label2 = upperString;

            } else {
                seekbar.setProgress(50);
                stateMap = localStateMap;
                float adjustmentValue = (50 - 1) * 2 / 100;
                progress = adjustmentValue;
                progressInt = (int) ((adjustmentValue + 1) / 2 * 100);
                String upperString = label.toString().substring(0, 1).toUpperCase() + label.toString().substring(1);
                label2 = upperString;
            }
        }
    }

    float progress;
    int progressInt;
    String label2;

    private void renderViewThread() {

        renderView.postDelayed(new Runnable() {
            @Override
            public void run() {
//                final Bitmap imageBm = scaledBitmap(decodeBitmapFromUri(ChooseActivity.this, myUri), renderRl.getWidth(), renderRl.getHeight());
                final Bitmap imageBm = decodeBitmapFromUri(ChooseActivity.this, myUri);
                inputWidth = imageBm.getWidth();
                inputHeight = imageBm.getHeight();
                new Thread() {
                    @Override
                    public void run() {
                        float perfectSize = 2000f;
                        float minScale = Math.min(perfectSize / imageBm.getWidth(), perfectSize / imageBm.getHeight());
                        minScale = Math.min(minScale, 1f);
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBm, (int) (minScale * imageBm.getWidth()), (int) (minScale * imageBm.getHeight()), true);
                        FaceUtil.InitFaceUtil(ChooseActivity.this);
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
                progressDialog.dismiss();

            }
        }, 1000);

    }


    @Override
    public void onBackPressed() {

        if (sliderCon.getVisibility() == View.VISIBLE) {
            sliderCon.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            bottomNavigation.setVisibility(View.VISIBLE);
            bottomLinear.setVisibility(View.GONE);


        } else {
//            super.onBackPressed();
            showExitDialog();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void DrawBitmap() {
        Bitmap surfaceBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888);
        PixelCopy.OnPixelCopyFinishedListener listener = new PixelCopy.OnPixelCopyFinishedListener() {
            @Override
            public void onPixelCopyFinished(int copyResult) {
                // success/failure callback
            }
        };

        PixelCopy.request(renderView, surfaceBitmap, listener, renderView.getHandler());
        // visualize the retrieved bitmap on your imageview
//        setImageBitmap(plotBitmap);
        saveBitmap(surfaceBitmap);
    }

    void saveBitmap(Bitmap bitmap) {
        Bitmap bm = bitmap;

        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/Photo Editor");
        myDir.mkdirs();
//        Random generator = new Random();
//        int n = 10000;
//        n = generator.nextInt(n);
        String fname = "IMG-" + now + ".jpg";
        File file = new File(myDir, fname);
//        Uri uri = Uri.fromFile(file);
        Uri uri = FileProvider.getUriForFile(this, "com.example.photoeditor3.fileprovider", file);
//            Log.i(TAG, "" + file);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
//            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            showSnackBar(uri);
            out.flush();
            out.close();
            addImage(file);
        } catch (Exception e) {
            Toast.makeText(this, "Exception", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    void showSnackBar(final Uri path) {
        Snackbar snackbar = Snackbar
                .make(chooseConstraintLayout, "Image Saved!", Snackbar.LENGTH_LONG)
                .setAction("View", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(Intent.ACTION_VIEW, path);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    }
                });
        snackbar.setActionTextColor(Color.RED);
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();
    }

    void showExitDialog() {
        MaterialDialog mDialog = new MaterialDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure?")
                .setCancelable(false)
                .setPositiveButton("Yes", new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        // Delete Operation
                        dialogInterface.dismiss();
                        finish();
                    }
                })
                .setNegativeButton("No", new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .build();

        // Show Dialog
        mDialog.show();
    }

    public Uri addImage(File imageFile) {
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.Images.Media.TITLE, "My image title");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    void setRecyclerView(ArrayList<String> featuresTextViewList, ArrayList<Integer> featuresImageList) {
        recyclerAdapter = new RecyclerAdapter(this, this, this);

        mRecyclerView.setAdapter(recyclerAdapter);

        recyclerAdapter.setFileNames(featuresTextViewList);
        recyclerAdapter.setImageViews(featuresImageList);
    }

    void setFilterRecyclerView(ArrayList<String> featuresTextViewList, ArrayList<Integer> featuresImageList) {
        recyclerAdapter = new RecyclerAdapter(this, this, this);

        mRecyclerView.setAdapter(recyclerAdapter);

        recyclerAdapter.setFileNames(featuresTextViewList);
        recyclerAdapter.setImageViews(featuresImageList);
    }

    void setProfessionalRecyclerView(ArrayList<String> featuresTextViewList, ArrayList<Integer> featuresImageList) {
        recyclerAdapter = new RecyclerAdapter(this, this, this);

        mRecyclerView.setAdapter(recyclerAdapter);

        recyclerAdapter.setFileNames(featuresTextViewList);
        recyclerAdapter.setImageViews(featuresImageList);
    }

    public void populateArraylists() {

        featuresTextViews.add("contrast");
        featuresTextViews.add("brightness");
        featuresTextViews.add("saturation");
        featuresTextViews.add("structure");
        featuresTextViews.add("sharpen");
        featuresTextViews.add("smooth");
        featuresTextViews.add("temperature");
        featuresTextViews.add("tint");
        featuresTextViews.add("highlights");
        featuresTextViews.add("shadows");

        featuresImageViews.add(R.drawable.contrast);
        featuresImageViews.add(R.drawable.brightness);
        featuresImageViews.add(R.drawable.saturation);
        featuresImageViews.add(R.drawable.structure);
        featuresImageViews.add(R.drawable.sharp);
        featuresImageViews.add(R.drawable.smooth);
        featuresImageViews.add(R.drawable.temp);
        featuresImageViews.add(R.drawable.tint);
        featuresImageViews.add(R.drawable.highlights);
        featuresImageViews.add(R.drawable.shadow);
    }

    public void populateProfessionalArraylists() {

        professionalTextViews.add("fringing");
        professionalTextViews.add("dehaze");
        professionalTextViews.add("whites");
        professionalTextViews.add("blacks");
        professionalTextViews.add("saturation_red");
        professionalTextViews.add("saturation_orange");
        professionalTextViews.add("saturation_yellow");
        professionalTextViews.add("saturation_green");
        professionalTextViews.add("saturation_aqua");
        professionalTextViews.add("saturation_blue");
//        professionalTextViews.add("saturation_purple");
//        professionalTextViews.add("saturation_magenta");
        professionalTextViews.add("luminance_red");
        professionalTextViews.add("luminance_orange");
        professionalTextViews.add("luminance_yellow");
        professionalTextViews.add("luminance_green");
        professionalTextViews.add("luminance_aqua");
        professionalTextViews.add("luminance_blue");
//        professionalTextViews.add("luminance_purple");
//        professionalTextViews.add("luminance_magenta");
//

        professionalImageViews.add(R.drawable.fringing);
        professionalImageViews.add(R.drawable.dehaze);
        professionalImageViews.add(R.drawable.whites);
        professionalImageViews.add(R.drawable.blacks);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);
        professionalImageViews.add(R.drawable.pic);

    }

    public void populateFilterArrayLists() {

        if (mFiltersList == null) {
            List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
            mFiltersList = new ArrayList<>();
            for (FilterPackage filterPackage : packages) {
                mFiltersList.addAll(filterPackage.filters);
            }
        }
        final CharSequence items[] = new CharSequence[mFiltersList.size()];
        for (int i = 0; i < mFiltersList.size(); i++) {
            items[i] = mFiltersList.get(i).filterName("zh");
            filterTextViews.add(mFiltersList.get(i).filterName("zh"));

        }

        filterImageViews.add(R.drawable.paris);
        filterImageViews.add(R.drawable.nature);
        filterImageViews.add(R.drawable.clear);
        filterImageViews.add(R.drawable.muse);
        filterImageViews.add(R.drawable.blue_lake);
        filterImageViews.add(R.drawable.smoothie);
        filterImageViews.add(R.drawable.t2);
        filterImageViews.add(R.drawable.m1_final);
        filterImageViews.add(R.drawable.black_white);
        filterImageViews.add(R.drawable.japenese_style);
        filterImageViews.add(R.drawable.sea_bubble);
        filterImageViews.add(R.drawable.vsco_t1);
        filterImageViews.add(R.drawable.mix_s109);
        filterImageViews.add(R.drawable.vsco_c1);
        filterImageViews.add(R.drawable.m3);
        filterImageViews.add(R.drawable.polarr_electric);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.save_image_btn) {
//            takeScreenshot();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                DrawBitmap();
            }
        } else if (v.getId() == R.id.choose_back_image_btn) {
            onBackPressed();
        }
    }
}
