package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.myapplication.helper.AppProgressDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.spi.FileTypeDetector;
import java.sql.Timestamp;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    public static File imageFile;
    public static String encodedImage;
    public static String resizedImage;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private ImageView imageCapture;
    private ImageView imageCloseCamera;
    private ImageView imageConfirm;
    private int cameraId;
    private int rotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        surfaceView = findViewById(R.id.surfaceView);
        imageConfirm = findViewById(R.id.iVConfirm);
        imageCloseCamera = findViewById(R.id.btnClose);
        imageCapture = findViewById(R.id.ivCaptureImage);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        imageCapture.setOnClickListener(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageCloseCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        imageConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AddCardActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!openCamera(Camera.CameraInfo.CAMERA_FACING_BACK)) {
        }
    }

    private boolean openCamera(int id) {
        boolean result = false;
        cameraId = id;
        releaseCamera();
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camera != null) {
            try {
                setUpCamera(camera);
                camera.setErrorCallback(new Camera.ErrorCallback() {

                    @Override
                    public void onError(int error, Camera camera) {

                    }
                });

                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                result = true;

            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                releaseCamera();
            }
        }
        return result;
    }

    private void setUpCamera(Camera c) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        rotation = getWindowManager().getDefaultDisplay().getRotation();

        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;

            default:
                break;
        }

        rotation = (info.orientation - degree + 360) % 360;
        c.setDisplayOrientation(rotation);

        Camera.Parameters params = camera.getParameters();

        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.setParameters(params);

        params.setRotation(rotation);
    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.setErrorCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error", e.toString());
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.ivCaptureImage) {
            takeImage();
        }
        camera.autoFocus(myAutofocusCallback);
    }

    Camera.AutoFocusCallback myAutofocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            imageCapture.setEnabled(true);
        }
    };

    private void takeImage() {

        AppProgressDialog.showProgressDialog(this, "Capturing...", false);

        camera.takePicture(null, null, new Camera.PictureCallback() {


            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                AppProgressDialog.hideProgressDialog();
                try {
                    // convert byte array into bitmap
                    Bitmap loadedImage = null;
                    Bitmap rotatedBitmap = null;
                    loadedImage = BitmapFactory.decodeByteArray(data, 0,
                            data.length);

                    // rotate Image
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(rotation);
                    rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
                            loadedImage.getWidth(), loadedImage.getHeight(),
                            rotateMatrix, false);
                    String state = Environment.getExternalStorageState();
                    File folder = null;

                    if (state.contains(Environment.MEDIA_MOUNTED)) {
                        folder = new File(Environment.getExternalStorageDirectory() + "/Skyapp");
                        Log.d("pathfolder", "aaaaaa" + folder);

                    } else {
                        folder = new File(Environment.getExternalStorageDirectory() + "/Skyapp");
                    }

                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        java.util.Date date = new java.util.Date();
                        imageFile = new File(folder.getAbsolutePath()
                                + File.separator
                                + new Timestamp(date.getTime()).toString()
                                + "Image.jpg");

                        imageFile.createNewFile();
                        Log.d("pathimagefile", "aaaaaa" + imageFile);


                    } else {
                        Toast.makeText(getBaseContext(), "Image Not saved",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                    // save image into gallery
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                    FileOutputStream fout = new FileOutputStream(imageFile);
                    fout.write(ostream.toByteArray());
                    fout.close();
                    ContentValues values = new ContentValues();

                    values.put(MediaStore.Images.Media.DATE_TAKEN,
                            System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA,
                            imageFile.getAbsolutePath());

                    CameraActivity.this.getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                imageCapture.setEnabled(false);
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

                encodedImage = encodeImage(imageFile);
                resizedImage = resizeBase64Image(encodedImage);
            }
        });

        Toast.makeText(getApplicationContext(), "Image Captured", Toast.LENGTH_SHORT).show();

        imageConfirm.setVisibility(View.VISIBLE);

    }

    public static String resizeBase64Image(String base64) {

        byte[] encodeByte = Base64.decode(base64.getBytes(), Base64.DEFAULT);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        Bitmap bm = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length, options);

        if (bm.getHeight() <= 400 && bm.getWidth() <= 400) {
            return base64;
        }
        bm = Bitmap.createScaledBitmap(bm, 400, 400, false);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        byte[] b = baos.toByteArray();
        System.gc();

        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public String encodeImage(File path) {

        Bitmap bm = BitmapFactory.decodeFile(imageFile.getPath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        AppProgressDialog.hideProgressDialog();
        return encodedImage;

    }

//    private AlertDialog.Builder createAlert(Context context, String title, String message) {
//
//        AlertDialog.Builder dialog = new AlertDialog.Builder(
//                new ContextThemeWrapper(context,
//                        android.R.style.Theme_Holo_Light_Dialog));
//
//        dialog.setIcon(R.drawable.ic_launcher_background);
//        if (title != null)
//            dialog.setTitle(title);
//        else
//            dialog.setTitle("Information");
//        dialog.setMessage(message);
//        dialog.setCancelable(false);
//        return dialog;
//    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


}