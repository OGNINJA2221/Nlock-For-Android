package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class CameraHelper(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Primary silent capture method using Android Native Camera2 API.
     * Works headlessly without needing an active UI preview surface or LifecycleOwner.
     */
    fun captureIntruderPhoto(
        onCaptured: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasCameraPermission()) {
            onError("Camera permission not granted")
            return
        }

        try {
            captureWithCamera2(onCaptured, onError)
        } catch (e: Exception) {
            Log.e("CameraHelper", "Camera2 capture initiation failed: ${e.message}", e)
            onError(e.message ?: "Failed to initiate silent camera capture")
        }
    }

    /**
     * Overload accepting LifecycleOwner for backwards compatibility with existing call-sites.
     */
    fun captureIntruderPhoto(
        lifecycleOwner: LifecycleOwner?,
        onCaptured: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasCameraPermission()) {
            onError("Camera permission not granted")
            return
        }

        // Try silent native Camera2 first; fallback to CameraX if needed
        try {
            captureWithCamera2(onCaptured) { camera2Err ->
                Log.w("CameraHelper", "Camera2 failed ($camera2Err), trying CameraX fallback...")
                if (lifecycleOwner != null) {
                    captureWithCameraX(lifecycleOwner, onCaptured, onError)
                } else {
                    onError(camera2Err)
                }
            }
        } catch (e: Exception) {
            if (lifecycleOwner != null) {
                captureWithCameraX(lifecycleOwner, onCaptured, onError)
            } else {
                onError(e.message ?: "Camera capture failed")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureWithCamera2(
        onCaptured: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            onError("Camera service unavailable")
            return
        }

        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) {
            onError("No camera available on device")
            return
        }

        var frontCameraId: String? = null
        var sensorOrientation = 270

        for (id in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                frontCameraId = id
                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 270
                break
            }
        }

        val targetCameraId = frontCameraId ?: cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(targetCameraId)
        if (frontCameraId == null) {
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
        }

        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()

        // Choose optimal size for quick silent capture
        val optimalSize = jpegSizes.firstOrNull { it.width in 640..1280 && it.height in 480..960 }
            ?: jpegSizes.firstOrNull()
            ?: Size(640, 480)

        val dir = File(context.filesDir, "intruders").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(dir, "INTRUDER_$timeStamp.jpg")

        val handlerThread = HandlerThread("IntruderCamera2Thread").apply { start() }
        val bgHandler = Handler(handlerThread.looper)
        val isCompleted = AtomicBoolean(false)

        var imageReader: ImageReader? = null
        var cameraDevice: CameraDevice? = null
        var captureSession: CameraCaptureSession? = null

        fun cleanup() {
            try {
                captureSession?.close()
                captureSession = null
            } catch (_: Exception) {}
            try {
                cameraDevice?.close()
                cameraDevice = null
            } catch (_: Exception) {}
            try {
                imageReader?.close()
                imageReader = null
            } catch (_: Exception) {}
            try {
                handlerThread.quitSafely()
            } catch (_: Exception) {}
        }

        // Safety timeout of 4.5 seconds
        bgHandler.postDelayed({
            if (isCompleted.compareAndSet(false, true)) {
                cleanup()
                mainHandler.post { onError("Camera capture timed out") }
            }
        }, 4500)

        val readerInstance = ImageReader.newInstance(optimalSize.width, optimalSize.height, ImageFormat.JPEG, 2)
        imageReader = readerInstance
        readerInstance.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    FileOutputStream(photoFile).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }
                    image.close()

                    if (isCompleted.compareAndSet(false, true)) {
                        cleanup()
                        Log.d("CameraHelper", "Intruder photo saved successfully: ${photoFile.absolutePath}")
                        mainHandler.post { onCaptured(photoFile.absolutePath) }
                    }
                } catch (e: Exception) {
                    image.close()
                    if (isCompleted.compareAndSet(false, true)) {
                        cleanup()
                        Log.e("CameraHelper", "Error saving photo bytes: ${e.message}", e)
                        mainHandler.post { onError(e.message ?: "Failed to save photo") }
                    }
                }
            }
        }, bgHandler)

        cameraManager.openCamera(targetCameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                try {
                    val surfaces = listOf(readerInstance.surface)
                    @Suppress("DEPRECATION")
                    camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            try {
                                val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                    addTarget(readerInstance.surface)
                                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                    set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation)
                                }
                                session.capture(captureBuilder.build(), null, bgHandler)
                            } catch (e: Exception) {
                                if (isCompleted.compareAndSet(false, true)) {
                                    cleanup()
                                    mainHandler.post { onError(e.message ?: "Failed to capture image") }
                                }
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            if (isCompleted.compareAndSet(false, true)) {
                                cleanup()
                                mainHandler.post { onError("Capture session configuration failed") }
                            }
                        }
                    }, bgHandler)
                } catch (e: Exception) {
                    if (isCompleted.compareAndSet(false, true)) {
                        cleanup()
                        mainHandler.post { onError(e.message ?: "Failed to create session") }
                    }
                }
            }

            override fun onDisconnected(camera: CameraDevice) {
                if (isCompleted.compareAndSet(false, true)) {
                    cleanup()
                    mainHandler.post { onError("Camera disconnected") }
                }
            }

            override fun onError(camera: CameraDevice, error: Int) {
                if (isCompleted.compareAndSet(false, true)) {
                    cleanup()
                    mainHandler.post { onError("Camera device error: $error") }
                }
            }
        }, bgHandler)
    }

    private fun captureWithCameraX(
        lifecycleOwner: LifecycleOwner,
        onCaptured: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)

                val dir = File(context.filesDir, "intruders").apply { if (!exists()) mkdirs() }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val photoFile = File(dir, "INTRUDER_$timeStamp.jpg")

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d("CameraHelper", "CameraX photo saved: ${photoFile.absolutePath}")
                            onCaptured(photoFile.absolutePath)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraHelper", "CameraX capture failed: ${exception.message}", exception)
                            onError(exception.message ?: "Capture failed")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraHelper", "CameraX error: ${e.message}", e)
                onError(e.message ?: "Camera provider error")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
