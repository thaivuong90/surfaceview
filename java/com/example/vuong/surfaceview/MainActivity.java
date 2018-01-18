package com.example.vuong.surfaceview;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Handler.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MSG_CAMERA_OPENED = 1;
    private static final int MSG_SURFACE_READY = 2;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1242;
    private static final int IMAGE_WIDTH  = 400;
    private static final int IMAGE_HEIGHT = 400;
    private static final int MAX_IMAGES   = 40;

    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    Surface mCameraSurface = null;

    CameraManager mCameraManager;
    CameraDevice mCameraDevice;
    String[] mCameraIDsList;

    boolean mSurfaceCreated = false;
    boolean mIsCameraConfigured = false;

    private final Handler mHandler = new Handler(this);
    private Handler mCameraHandler;
    private HandlerThread mCameraThread;

    private CameraDevice.StateCallback mCameraStateCB;
    private CameraCaptureSession mCaptureSession;

    // Image result processor
    private ImageReader mImageReader;

    private ImageView imageView;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.btnCapture);
        imageView = (ImageView) findViewById(R.id.imageView);
        mSurfaceView = (SurfaceView) findViewById(R.id.cameraView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraIDsList = this.mCameraManager.getCameraIdList();
            for (String id : mCameraIDsList) {
                Log.v(TAG, "CameraID: " + id);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        // Initialize image processor
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.JPEG, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);

        mCameraStateCB = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Toast.makeText(getApplicationContext(), "onOpened", Toast.LENGTH_SHORT).show();

                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                Toast.makeText(getApplicationContext(), "onDisconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Toast.makeText(getApplicationContext(), "onError", Toast.LENGTH_SHORT).show();
            }
        };

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        //requesting permission
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                Toast.makeText(getApplicationContext(), "request permission", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "PERMISSION_ALREADY_GRANTED", Toast.LENGTH_SHORT).show();
            try {
                mCameraManager.openCamera(mCameraIDsList[0], mCameraStateCB, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCameraThread.quitSafely();

        shutDown();
    }

    // Close the camera resources
    public void shutDown() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            // Get the raw image bytes
            Image image = reader.acquireLatestImage();
            ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
            final byte[] imageBytes = new byte[imageBuf.remaining()];
            imageBuf.get(imageBytes);
            image.close();

            if (imageBytes != null) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

//                        Intent intent = new Intent(getApplicationContext(), PreviewActivity.class);
//
//                        intent.putExtra("image", imageBytes);
//                        startActivity(intent);

                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                        imageView.setImageBitmap(bitmap);

                        configureCamera();

                    }
                });


            }
        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCameraSurface = surfaceHolder.getSurface();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mCameraSurface = surfaceHolder.getSurface();
        mSurfaceCreated = true;
        mHandler.sendEmptyMessage(MSG_SURFACE_READY);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mSurfaceCreated = false;
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_CAMERA_OPENED:
            case MSG_SURFACE_READY:
                // if both surface is created and camera device is opened
                // - ready to set up preview and other things
                if (mSurfaceCreated && (mCameraDevice != null)
                        && !mIsCameraConfigured) {
                    configureCamera();
                }
                break;
        }

        return true;
    }

    private void takePicture() {
        if (mCameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Camera not initialized.");
            return;
        }

        // Here, we create a CameraCaptureSession for capturing still images.
        try {
            mCameraDevice.createCaptureSession(
                    Collections.singletonList(mImageReader.getSurface()),
                    mSessionCallback2,
                    null);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "access exception while preparing pic", cae);
        }
    }

    private void configureCamera() {
        // prepare list of surfaces to be used in capture requests
        List<Surface> sfl = new ArrayList<Surface>();

        sfl.add(mCameraSurface); // surface for viewfinder preview

        // configure camera with all the surfaces to be ever used
        try {
            mCameraDevice.createCaptureSession(sfl,
                    mSessionCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mIsCameraConfigured = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    try {
                        mCameraManager.openCamera(mCameraIDsList[0], mCameraStateCB, new Handler());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                break;
        }
    }

    private CameraCaptureSession.StateCallback mSessionCallback =
        new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigureFailed(final CameraCaptureSession session) {
            Log.d(TAG, "CaptureSessionConfigure failed");
        }

        @Override
        public void onConfigured(final CameraCaptureSession session) {
            Log.d(TAG, "CaptureSessionConfigure onConfigured");
            mCaptureSession = session;

            try {
                CaptureRequest.Builder previewRequestBuilder = mCameraDevice
                        .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(mCameraSurface);
//                previewRequestBuilder.addTarget(mImageReader.getSurface());
//                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
//                mCaptureSession.capture(previewRequestBuilder.build(), mCaptureCallback, null);
                mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                        null, null);
            } catch (CameraAccessException e) {
                Log.d(TAG, "setting up preview failed");
                e.printStackTrace();
            }
        }
    };

    // Callback handling session state changes
    private CameraCaptureSession.StateCallback mSessionCallback2 =
        new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            // The camera is already closed
            if (mCameraDevice == null) {
                return;
            }

            // When the session is ready, we start capture.
            mCaptureSession = cameraCaptureSession;
            try {
                final CaptureRequest.Builder captureBuilder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(mImageReader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                Log.d(TAG, "Session initialized.");
                mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);
            } catch (CameraAccessException cae) {
                Log.d(TAG, "camera capture exception");
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            Log.w(TAG, "Failed to configure camera");
        }
    };

    // Callback handling capture progress events
    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
            if (session != null) {
                session.close();
                mCaptureSession = null;
                Log.d(TAG, "CaptureSession closed");
            }
        }
    };
}
