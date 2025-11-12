package com.tangem.features.kyc.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.kyc.SumSubWebViewClient
import com.tangem.features.kyc.entity.WebSdkKycUM

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
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(context.isCameraGranted()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionGranted = it[Manifest.permission.CAMERA] ?: context.isCameraGranted()
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permission.launch(arrayOf(Manifest.permission.CAMERA))
    }

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
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.grant(request.resources)
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