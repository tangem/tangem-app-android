package com.tangem.features.kyc.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.kyc.SumSubWebViewClient
import com.tangem.features.kyc.entity.WebSdkKycUM

private const val GALLERY_IMAGE_FILTER = "image/*"

@Composable
internal fun KycWebViewScreen(state: WebSdkKycUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        AppBarWithBackButton(
            modifier = Modifier.statusBarsPadding(),
            onBackClick = state.onBackClick,
            text = TextReference.EMPTY.resolveReference(),
            containerColor = TangemTheme.colors.background.primary,
        )
        WebViewContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = state,
        )
    }
}

@Composable
private fun WebViewContent(state: WebSdkKycUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !state.isLoading && !state.accessToken.isNullOrEmpty() -> {
                SumSubContent(state.accessToken, state.url)
            }
            else -> {
                SumSubLoading()
            }
        }
    }
}

@Composable
private fun SumSubLoading(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = TangemTheme.colors.icon.primary1,
    )
}

@Composable
private fun SumSubContent(accessToken: String, url: String, modifier: Modifier = Modifier) {
    val sumSubWebViewClient = remember { SumSubWebViewClient(accessToken = accessToken) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            pendingPermissionRequest?.let { request ->
                if (isGranted) {
                    request.grant(request.resources)
                } else {
                    request.deny()
                }
            }
            pendingPermissionRequest = null
        }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            filePathCallback?.onReceiveValue(arrayOf(uri))
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request == null) return
                        val requestedCamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                        when {
                            !requestedCamera || context.isCameraGranted() -> {
                                request.grant(request.resources)
                            }
                            else -> {
                                pendingPermissionRequest = request
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                    override fun onShowFileChooser(
                        webView: WebView?,
                        callback: ValueCallback<Array<Uri>>?,
                        params: FileChooserParams?,
                    ): Boolean {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback
                        galleryLauncher.launch(GALLERY_IMAGE_FILTER)
                        return true
                    }
                }
                webViewClient = sumSubWebViewClient
                loadUrl(url)
            }
        },
    )
}

private fun Context.isCameraGranted() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED