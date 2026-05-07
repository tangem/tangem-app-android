package com.tangem.features.walletconnect.pay.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.pay.entity.PaymentOptionUM
import com.tangem.features.walletconnect.pay.entity.WcPayUM
import org.json.JSONObject

@Composable
internal fun WcPayBottomSheet(state: WcPayUM, onKycComplete: () -> Unit, onKycFail: () -> Unit) {
    if (state.state == WcPayUM.State.KYC_WEBVIEW) {
        KycFullScreenDialog(
            url = state.kycUrl.orEmpty(),
            onComplete = onKycComplete,
            onError = onKycFail,
            onDismiss = state.onDismiss,
        )
    }

    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = state.state != WcPayUM.State.KYC_WEBVIEW,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        title = { TangemModalBottomSheetTitle(title = stringReference("Wallet Connect Pay")) },
        content = {
            when (state.state) {
                WcPayUM.State.LOADING -> LoadingContent()
                WcPayUM.State.OPTIONS -> OptionsContent(state)
                WcPayUM.State.SIGNING -> SigningContent(
                    current = state.signingCurrent,
                    total = state.signingTotal,
                )
                WcPayUM.State.SUCCESS -> ResultContent(isSuccess = true, state = state)
                WcPayUM.State.FAILED -> ResultContent(isSuccess = false, state = state)
                else -> {}
            }
        },
        footer = { WcPayFooter(state) },
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TangemTheme.colors.icon.primary1)
    }
}

@Composable
private fun OptionsContent(state: WcPayUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        MerchantInfoRow(
            name = state.merchantName,
            iconUrl = state.merchantIconUrl,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.paymentAmount,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        state.options.forEach { option ->
            PaymentOptionItem(
                option = option,
                onSelect = { state.onOptionSelected(option.id) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MerchantInfoRow(name: String, iconUrl: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = name,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun PaymentOptionItem(option: PaymentOptionUM, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TangemTheme.colors.background.primary)
            .then(
                if (option.isSelected) {
                    Modifier.border(2.dp, TangemTheme.colors.icon.accent, shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onSelect)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (option.tokenIconUrl != null) {
            AsyncImage(
                model = option.tokenIconUrl,
                contentDescription = option.tokenSymbol,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${option.amount} ${option.tokenSymbol}",
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = option.networkName,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }

        if (option.requiresKyc) {
            Text(
                text = "Info required",
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun KycFullScreenDialog(url: String, onComplete: () -> Unit, onError: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KYC_BACKGROUND_COLOR),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                KycWebView(
                    url = url,
                    onComplete = onComplete,
                    onError = onError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            CloseButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun KycWebView(url: String, onComplete: () -> Unit, onError: () -> Unit, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val matchParent = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            FrameLayout(ctx).apply {
                layoutParams = matchParent

                val webView = WebView(ctx).apply {
                    layoutParams = matchParent
                    setBackgroundColor(KYC_BACKGROUND_COLOR_INT)
                    isNestedScrollingEnabled = false
                    overScrollMode = android.view.View.OVER_SCROLL_NEVER
                    isFocusable = true
                    isFocusableInTouchMode = true

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    addJavascriptInterface(
                        WcPayJsBridge(onComplete = onComplete, onError = onError),
                        "AndroidWallet",
                    )

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                            false
                    }

                    loadUrl(url)
                }
                addView(webView)
            }
        },
    )
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = "✕",
        style = TangemTheme.typography.h3,
        color = TangemTheme.colors.text.primary1,
        modifier = modifier
            .size(CLOSE_BUTTON_SIZE)
            .background(
                color = Color.White.copy(alpha = CLOSE_BUTTON_ALPHA),
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .wrapContentSize(Alignment.Center),
    )
}

private val KYC_BACKGROUND_COLOR = Color(0xFF141414)
private const val KYC_BACKGROUND_COLOR_INT = 0xFF141414.toInt()
private val CLOSE_BUTTON_SIZE = 36.dp
private const val CLOSE_BUTTON_ALPHA = 0.8f

private class WcPayJsBridge(
    private val onComplete: () -> Unit,
    private val onError: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onDataCollectionComplete(json: String) {
        val type = runCatching { JSONObject(json).optString("type") }.getOrNull()
        mainHandler.post {
            when (type) {
                "IC_COMPLETE" -> onComplete()
                "IC_ERROR" -> onError()
            }
        }
    }
}

@Composable
private fun SigningContent(current: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = TangemTheme.colors.icon.primary1)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (total > 1) "Signing $current/$total..." else "Processing payment...",
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Composable
private fun ResultContent(isSuccess: Boolean, state: WcPayUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isSuccess) "Payment Successful" else "Payment Failed",
            style = TangemTheme.typography.h3,
            color = if (isSuccess) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.warning,
        )
        if (isSuccess) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${state.paymentAmount} ${state.paymentCurrency}",
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.secondary,
            )
            Text(
                text = state.merchantName,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        if (!isSuccess && state.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun BoxScope.WcPayFooter(state: WcPayUM) {
    when (state.state) {
        WcPayUM.State.OPTIONS -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = state.onDismiss,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Continue",
                    onClick = state.onContinue,
                    enabled = state.selectedOptionId != null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        WcPayUM.State.SUCCESS,
        WcPayUM.State.FAILED,
        -> {
            PrimaryButton(
                text = "Done",
                onClick = state.onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        else -> { /* no footer */ }
    }
}