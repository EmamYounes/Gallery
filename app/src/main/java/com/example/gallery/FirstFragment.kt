package com.example.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gallery.databinding.FragmentFirstBinding
import java.util.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    private val binding get() = _binding!!

    private lateinit var backgroundThread: HandlerThread

    private lateinit var backgroundHandler: Handler

    private lateinit var cameraDevice: CameraDevice

    private val MAX_PREVIEW_WIDTH = 1920
    private val MAX_PREVIEW_HEIGHT = 1080
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            previewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            this@FirstFragment.activity?.finish()
        }

    }

    private fun previewSession() {
        val surfaceTexture = binding.textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        val surface = Surface(surfaceTexture)
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        cameraDevice.createCaptureSession(
            mutableListOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)

                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "CameraCaptureSession ConfigureFailed")
                }
            },
            null
        )
    }

    private fun closeCamera() {
        if (this::captureSession.isInitialized)
            captureSession.close()

        if (this::cameraDevice.isInitialized)
            cameraDevice.close()


    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("camera2 kotlin").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        backgroundThread.join()
    }

    private val cameraManager by lazy {
        activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun <T> cameraCharacteristic(cameraId: String, key: CameraCharacteristics.Key<T>): T? {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.AUTOMOTIVE_LENS_FACING -> characteristics.get(key)
            else -> throw java.lang.IllegalArgumentException("key not recognize")
        }
    }

//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    private fun cameraID(lens: Int): String {
//        var deviceID = listOf<String>()
//        deviceID = cameraManager.cameraIdList.filter {
//            lens == cameraCharacteristic(it)
//        }
//    }

    companion object {

        const val REQUEST_CAMERA_PERMISSION = 100

        private val TAG = FirstFragment::class.qualifiedName

    }

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "texture width:$width height :$height")
            openCamera(width, height)

        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
//            configureTransform(width, height);

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {

        val deviceID = cameraManager.cameraIdList
        cameraManager.openCamera(deviceID[0], deviceStateCallback, backgroundHandler)
    }

    private fun bindView() {

        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.CAMERA
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA
                ), REQUEST_CAMERA_PERMISSION
            )
        } else {
            handleCamera()
        }

        binding.captureIcon.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        binding.backIcon.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun handleCamera() {
        if (binding.textureView.isAvailable)
            openCamera(binding.textureView.width, binding.textureView.height)
        else
            binding.textureView.surfaceTextureListener = surfaceListener
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        bindView()
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
        stopBackgroundThread()
    }

    @Deprecated("onRequestPermissionsResult Deprecated")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    handleCamera()
                }

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(activity?.window!!, false)
        WindowInsetsControllerCompat(requireActivity().window!!, requireView()).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window!!, true)
        WindowInsetsControllerCompat(
            activity?.window!!,
            requireView()
        ).show(WindowInsetsCompat.Type.systemBars())
    }

}