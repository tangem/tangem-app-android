package com.tangem.features.onramp.success.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.common.ui.expressStatus.ExpressStatusBlock
import com.tangem.common.ui.expressStatus.ExpressStatusNotificationBlock
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import com.tangem.features.onramp.success.entity.previewdata.OnrampSuccessComponentUMPreviewData

@Composable
internal fun OnrampSuccessComponentContent(
    state: OnrampSuccessComponentUM,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state !is OnrampSuccessComponentUM.Content) {
        Box(modifier.background(TangemTheme.colors.background.secondary))
    } else {
        Content(
            state = state,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun Content(state: OnrampSuccessComponentUM.Content, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary)
            .systemBarsPadding(),
        topBar = {
            TangemTopAppBar(
                startButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_close_24,
                    onIconClicked = onBackClick,
                ),
            )
        },
        bottomBar = {
            PrimaryButton(
                text = stringResourceSafe(R.string.common_close),
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        },
        contentWindowInsets = WindowInsetsZero,
    ) { scaffoldPaddings ->
        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.secondary)
                .padding(scaffoldPaddings)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            TransactionDoneTitle(
                title = resourceReference(R.string.common_in_progress),
                subtitle = resourceReference(
                    R.string.send_date_format,
                    wrappedList(
                        state.timestamp.toTimeFormat(DateTimeFormatters.dateFormatter),
                        state.timestamp.toTimeFormat(),
                    ),
                ),
            )
            SpacerH24()
            AmountBlock(state)
            SpacerH12()
            FooterContainer(
                footer = resourceReference(R.string.onramp_transaction_status_footer_text),
            ) {
                ExpressStatusBlock(state.statusBlock)
            }
            ExpressStatusNotificationBlock(
                state.notification,
            )
        }
    }
}

@Composable
private fun AmountBlock(state: OnrampSuccessComponentUM.Content) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .padding(16.dp),
    ) {
        val iconModifier = Modifier.size(size = 40.dp)
        SubcomposeAsyncImage(
            modifier = iconModifier,
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(state.currencyImageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { CircleShimmer(modifier = iconModifier) },
            error = { },
            contentDescription = null,
        )
        ResizableText(
            text = state.fromAmount.resolveReference(),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        Text(
            text = state.toAmount.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        SpacerH12()
        AmountProviderBlock(
            state.providerName,
            state.providerImageUrl,
        )
    }
}

@Composable
private fun AmountProviderBlock(providerName: TextReference, providerImageUrl: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_with),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
        SubcomposeAsyncImage(
            modifier = Modifier.size(size = 16.dp),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(providerImageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { CircleShimmer() },
            error = { },
            contentDescription = null,
        )
        Text(
            text = providerName.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnrampSuccessComponentContent_Preview(
    @PreviewParameter(OnrampSuccessComponentContentPreviewProvider::class)
    data: OnrampSuccessComponentUM,
) {
    TangemThemePreview {
        OnrampSuccessComponentContent(
            state = data,
            onBackClick = {},
        )
    }
}

private class OnrampSuccessComponentContentPreviewProvider :
    PreviewParameterProvider<OnrampSuccessComponentUM> {
    override val values: Sequence<OnrampSuccessComponentUM>
        get() = sequenceOf(
            OnrampSuccessComponentUMPreviewData.contentState,
        )
}
// endregion