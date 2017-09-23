
package com.reactlibrary;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import static android.hardware.Camera.open;
import static android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE;

public class RNLitModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    private final ReactApplicationContext reactContext;

    private boolean deviceSupportsFlash;
    private CameraManager.TorchCallback torchCallback;
    private String cameraIdWithFlash;
    private CameraManager camManager;
    private static Camera camera;
    private Promise onOffPromise;

    private static final String ERROR_FLASHLIGHT_NOT_AVAILABLE = "500";
    private static final String ERROR_CHANGING_TORCH_MODE = "501";
    private static final String ERROR_CANNOT_ACCESS_CAMERA = "502";

    public RNLitModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            torchCallback = new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeUnavailable(@NonNull String cameraId) {
                    super.onTorchModeUnavailable(cameraId);
                    if (onOffPromise != null) {
                        onOffPromise.reject(ERROR_FLASHLIGHT_NOT_AVAILABLE, "Flashlight is not currently available.");
                    }
                }

                @Override
                public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                    super.onTorchModeChanged(cameraId, enabled);
                    WritableMap obj = Arguments.createMap();
                    obj.putBoolean("isEnabled", enabled);
                    if (onOffPromise != null) {
                        onOffPromise.resolve(obj);
                    }
                }
            };
            this.camManager = (CameraManager) reactContext.getSystemService(Context.CAMERA_SERVICE);
        }
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return "RNLit";
    }

    @ReactMethod
    public void isFlashAvail(Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.camManager = (CameraManager) reactContext.getSystemService(Context.CAMERA_SERVICE);
            // find the first camera that supports flash
            String[] cameraIds = null;
            try {
                cameraIds = camManager.getCameraIdList();
            } catch (CameraAccessException e) {
                e.printStackTrace();
                promise.reject(ERROR_CANNOT_ACCESS_CAMERA, e.getMessage());
            }

            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = null;
                try {
                    characteristics = camManager.getCameraCharacteristics(cameraId);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    promise.reject(ERROR_CANNOT_ACCESS_CAMERA, e.getMessage());
                }

                if (characteristics != null && characteristics.get(FLASH_INFO_AVAILABLE)) {
                    this.cameraIdWithFlash = cameraId;
                    this.deviceSupportsFlash = true;
                    break;
                }
            }
        } else {
            this.deviceSupportsFlash = reactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        }

        WritableMap obj = Arguments.createMap();
        obj.putBoolean("deviceSupportsFlash", this.deviceSupportsFlash);
        promise.resolve(obj);
    }

    @ReactMethod
    public void turnOn(final boolean turnOnNow, final Promise promise) {
        this.onOffPromise = promise;
        if (!this.deviceSupportsFlash) {
            onOffPromise.reject(ERROR_FLASHLIGHT_NOT_AVAILABLE, "Flash is not available");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                this.camManager.setTorchMode(cameraIdWithFlash, turnOnNow);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                promise.reject(ERROR_CHANGING_TORCH_MODE, e.getMessage());
            }
        } else {
            if (camera == null) {
                Log.w(getClass().getSimpleName(), "Camera failed to open");
                onOffPromise.reject(ERROR_CANNOT_ACCESS_CAMERA, "Camera is not available");
                return;
            }

            final Camera.Parameters p = camera.getParameters();
            WritableMap obj = Arguments.createMap();
            if (turnOnNow) {
                if (p.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                } else if (p.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_ON)) {
                    p.setFlashMode((Camera.Parameters.FLASH_MODE_ON));
                }
                camera.setParameters(p);
                camera.startPreview();
            } else {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(p);
                camera.stopPreview();
            }
            obj.putBoolean("isEnabled", turnOnNow);
            onOffPromise.resolve(obj);
        }
    }

    @Override
    public void onHostResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.camManager != null) {
                this.camManager.registerTorchCallback(torchCallback, null);
            }
        } else {
            camera = open();
        }
    }

    @Override
    public void onHostPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.camManager != null) {
                onOffPromise = null;
                this.camManager.unregisterTorchCallback(torchCallback);
            }
        } else {
            camera.release();
        }
    }

    @Override
    public void onHostDestroy() {
        //no-op
    }
}
