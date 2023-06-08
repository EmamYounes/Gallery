package com.example.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.gallery.databinding.ActivityCustomCameraUiBinding
import io.reactivex.disposables.Disposable

class CustomCameraUI : Fragment() {

    private lateinit var camera2: Camera2
    private var disposable: Disposable? = null

    lateinit var binding: ActivityCustomCameraUiBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityCustomCameraUiBinding.inflate(inflater, container, false)
        init()
        return binding.root
    }

    private fun init() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )

            initCamera2Api()
        else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 3
            )

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            3 -> initCamera2Api()

        }
    }

    private fun initCamera2Api() {

        camera2 = Camera2(requireActivity(), binding.cameraView)

        binding.ivCaptureImage.setOnClickListener { v ->
            camera2.takePhoto {
                Toast.makeText(v.context, "Saving Picture", Toast.LENGTH_SHORT).show()
                disposable = Converters.convertBitmapToFile(it) { file ->
                    Toast.makeText(v.context, "Saved Picture Path ${file.path}", Toast.LENGTH_SHORT)
                        .show()
                }

            }


        }

    }


    override fun onPause() {
        //  cameraPreview.pauseCamera()
        if (this::camera2.isInitialized)
        camera2.close()
        super.onPause()
    }

    override fun onResume() {
        // cameraPreview.resumeCamera()
        if (this::camera2.isInitialized)
            camera2.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        if (disposable != null)
            disposable!!.dispose()
        super.onDestroy()
    }


}
