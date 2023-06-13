package com.tangem.tap.features.onboarding.products.wallet.saltPay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.tangem.tap.common.ActivityResultCallbackHolder
import com.tangem.tap.common.OnActivityResultCallback
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.isPermissionGranted
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import java.io.File

/**
 * Created by Anton Zhilenkov on 16.10.2022.
 */
class UtorgWebViewClient(
    private val onSuccess: () -> Unit,
    private val successUrl: String,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        if (successUrl == request.url.toString()) {
            onSuccess()
            true
        } else {
            false
        }
}

class UtorgWebChromeClient(
    private val activity: ComponentActivity,
    private val activityResultCallbackHolder: ActivityResultCallbackHolder = activity as ActivityResultCallbackHolder,
) : WebChromeClient() {

    private val fileChooseRequestCode = 1002
    private val permissionRequestCode = 1003
    private val onActivityResultCallback: OnActivityResultCallback = ::onActivityResult

    private val permissionList = listOf(
        "android.permission.CAMERA",
    )

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var mediaUri: Uri? = null

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        activityResultCallbackHolder.addOnActivityResultCallback(onActivityResultCallback)
        if (!readyToShowFileChooser()) return false

        val type: String
        if (fileChooserParams != null && fileChooserParams.acceptTypes != null &&
            fileChooserParams.acceptTypes.isNotEmpty()
        ) {
            type = if (!TextUtils.isEmpty(fileChooserParams.acceptTypes[0])) fileChooserParams.acceptTypes[0] else "*/*"
            this.filePathCallback = filePathCallback
        } else {
            return false
        }
        proceedOnType(type, fileChooserParams.isCaptureEnabled)

        return true
    }

    private fun readyToShowFileChooser(): Boolean {
        val permissionsToGrant = permissionList.filterNot { activity.isPermissionGranted(it) }.toTypedArray()
        return if (permissionsToGrant.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToGrant, permissionRequestCode)
            false
        } else {
            true
        }
    }

    private fun proceedOnType(type: String, isCaptureEnabled: Boolean) {
        if (isCaptureEnabled) {
            mediaUri = Uri.fromFile(File(getImageCaptureCachePath()))
            val imageCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            imageCaptureIntent.putExtra("output", mediaUri)
            activity.startActivityForResult(
                Intent.createChooser(imageCaptureIntent, "Image Chooser"),
                fileChooseRequestCode,
            )
        } else {
            mediaUri = Uri.fromFile(File(getImageCaptureCachePath()))

            val getContentIntent = Intent(Intent.ACTION_GET_CONTENT)
            getContentIntent.addCategory(Intent.CATEGORY_OPENABLE)
            getContentIntent.type = "image/*"

            // val imageCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            // imageCaptureIntent.putExtra("output", mediaUri)

            val chooserIntent = Intent.createChooser(getContentIntent, "Pick a photo")
            // chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(imageCaptureIntent))

            activity.startActivityForResult(
                Intent.createChooser(chooserIntent, "Image Chooser"),
                fileChooseRequestCode,
            )
        }
    }

    private fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            fileChooseRequestCode -> {
                if (resultCode != Activity.RESULT_OK) {
                    filePathCallback?.onReceiveValue(null)
                    clean()
                    return
                }
                if (data == null) {
                    filePathCallback?.onReceiveValue(arrayOf(mediaUri!!))
                } else {
                    val uri = FileChooserParams.parseResult(resultCode, data)
                    filePathCallback?.onReceiveValue(uri)
                }
                clean()
            }
            permissionRequestCode -> {
                val notGranted = permissionList.filterNot { activity.isPermissionGranted(it) }
                if (notGranted.isNotEmpty()) {
                    val permissionsName = notGranted.joinToString("\n") { it.permissionName() }
                    store.dispatchDialogShow(
                        AppDialog.SimpleOkDialog(
                            header = activity.getString(R.string.common_warning),
                            message = "Permission access is required to pass authorization." +
                                "To grant permission \n" +
                                "$permissionsName \n" +
                                "you must enable them in the app's settings",
                        ),
                    )
                }
            }
        }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        request.grant(request.resources)
    }

    private fun getImageCaptureCachePath(): String = "cached_photo_" + System.currentTimeMillis() + ".jpg"

    private fun clean() {
        activityResultCallbackHolder.removeOnActivityResultCallback(onActivityResultCallback)
        filePathCallback = null
        mediaUri = null
    }

    private fun String.permissionName(): String {
        return this.slice(this.lastIndexOf(".") until this.length)
    }
}
