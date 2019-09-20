package com.tangem.ui.fragment.qr

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.tangem.ui.fragment.BaseFragment
import com.tangem.wallet.R

class CameraPermissionManager(val fragment: BaseFragment) {

    fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) ==
                PermissionChecker.PERMISSION_GRANTED
    }

    fun handleRequestPermissionResult(requestCode: Int, grantResults: IntArray, action: () -> Unit) {
        when (requestCode) {
            1 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(fragment.requireContext(), R.string.general_toast_no_permission, Toast.LENGTH_LONG).show()
                } else {
                    action.invoke()
                }
            }
        }
    }

    fun requirePermission() {
        fragment.requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
    }

}