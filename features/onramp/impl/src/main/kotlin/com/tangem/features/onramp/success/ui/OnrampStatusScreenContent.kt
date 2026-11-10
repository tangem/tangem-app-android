package com.tangem.features.onramp.success.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.common.ui.expressStatus.ExpressStatusNotificationBlock
import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.test.TransactionSuccessScreenTestTags
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.core.ui.R as CoreUiR
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import com.tangem.features.onramp.success.entity.previewdata.OnrampSuccessComponentUMPreviewData

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
internal fun OnrampStatusScreenContent(
    state: OnrampSuccessComponentUM,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state !is OnrampSuccessComponentUM.Content) {
        TangemThemeRedesign {
            TangemStatusScreen(
                modifier = modifier,
                variant = TangemStatusScreen.Variant.Neutral,
                title = resourceReference(CoreUiR.string.common_in_progress),
                animated = false,
            )
        }
        return
    }

    TangemThemeRedesign {
        TangemStatusScreen(
            modifier = modifier,
            variant = state.activeStatus.toScreenVariant(),
            title = state.statusTitle,
            description = state.timestampDescription(),
            asset = { StatusAsset(status = state.activeStatus) },
            details = { Details(state = state) },
            footer = { Footer(onCloseClick = onCloseClick) },
        )
    }
}

private fun OnrampStatus.Status.toScreenVariant(): TangemStatusScreen.Variant = when (this) {
    OnrampStatus.Status.Finished -> TangemStatusScreen.Variant.Success
    OnrampStatus.Status.Failed,
    OnrampStatus.Status.Expired,
    OnrampStatus.Status.Refunded,
    -> TangemStatusScreen.Variant.Error
    OnrampStatus.Status.Verifying,
    OnrampStatus.Status.RefundInProgress,
    OnrampStatus.Status.Paused,
    -> TangemStatusScreen.Variant.Warning
    else -> TangemStatusScreen.Variant.Neutral
}

@Composable
private fun StatusAsset(status: OnrampStatus.Status) {
    val modifier = Modifier.size(40.dp)
    when (status.toScreenVariant()) {
        TangemStatusScreen.Variant.Success ->
            TangemStatusIcon(kind = TangemStatusIcon.Kind.Checkmark, modifier = modifier)
        TangemStatusScreen.Variant.Error ->
            TangemStatusIcon(kind = TangemStatusIcon.Kind.HeartBroken, modifier = modifier)
        TangemStatusScreen.Variant.Warning ->
            TangemStatusIcon(kind = TangemStatusIcon.Kind.Warning, modifier = modifier)
        else -> TangemLoader(size = TangemLoaderSize.X32, color = TangemTheme.colors3.icon.primary)
    }
}

private fun OnrampSuccessComponentUM.Content.timestampDescription(): TextReference = resourceReference(
    id = CoreUiR.string.send_date_format,
    formatArgs = wrappedList(
        timestamp.toTimeFormat(DateTimeFormatters.dateFormatter),
        timestamp.toTimeFormat(),
    ),
)

@Composable
private fun Details(state: OnrampSuccessComponentUM.Content) {
    state.notification?.let { ExpressStatusNotificationBlock(it) }
    Column(modifier = Modifier.fillMaxWidth()) {
        DetailRow(
            title = resourceReference(R.string.onramp_title_you_get),
            value = state.toAmount,
            divider = true,
        )
        DetailRow(
            title = resourceReference(R.string.onramp_you_will_pay_title),
            value = state.fromAmount,
            divider = true,
        )
        ProviderRow(name = state.providerName, imageUrl = state.providerImageUrl)
        state.externalTxId?.let { externalTxId ->
            DetailRow(
                title = resourceReference(CoreUiR.string.common_transaction_id),
                value = stringReference(externalTxId),
                onClick = (state.statusBlock.link as? ExpressLinkUM.Content)?.onClick,
            )
        }
    }
}

@Composable
private fun DetailRow(
    title: TextReference,
    value: TextReference,
    divider: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    TangemRow(
        divider = divider,
        onClick = onClick,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        valueSlot = {
            Text(
                text = value.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
            )
        },
    )
}

@Composable
private fun ProviderRow(name: TextReference, imageUrl: String) {
    TangemRow(
        divider = true,
        titleSlot = {
            TangemRowText(
                text = resourceReference(CoreUiR.string.express_provider),
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                )
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .testTag(TransactionSuccessScreenTestTags.PROVIDER_ICON),
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(imageUrl)
                        .crossfade(enable = true)
                        .allowHardware(false)
                        .build(),
                    loading = {
                        ProvideTangemShimmer { TangemShimmer(modifier = Modifier.size(16.dp), radius = 8.dp) }
                    },
                    error = { },
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun Footer(onCloseClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = resourceReference(R.string.onramp_transaction_status_footer_text).resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.tertiary,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.common_close),
            onClick = onCloseClick,
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnrampStatusScreenContentPreview() {
    TangemThemePreviewRedesign {
        OnrampStatusScreenContent(
            state = OnrampSuccessComponentUMPreviewData.contentState,
            onCloseClick = {},
        )
    }
}