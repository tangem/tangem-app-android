package com.tangem.ui.fragment.id

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import com.tangem.ui.fragment.BaseFragment
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_camera.*


class CameraFragment : BaseFragment() {
    override val layoutId = R.layout.fragment_camera

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cvCamera?.setLifecycleOwner(viewLifecycleOwner)

        cvCamera?.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) { // Picture was taken!

                result.toBitmap(200, 200) {
                    navigateBackWithResult(Activity.RESULT_OK, bundleOf(PHOTO_KEY to it))
                }
            }
        })
        ivTakePicture?.setOnClickListener { cvCamera?.takePictureSnapshot() }
        ivFlipCamera?.setOnClickListener { flipCamera() }
    }

    private fun flipCamera() {
        if (cvCamera?.facing == Facing.BACK) {
            cvCamera?.facing = Facing.FRONT
        } else {
            cvCamera?.facing = Facing.BACK
        }
    }

    companion object{
        const val PHOTO_KEY = "photo"
    }

}