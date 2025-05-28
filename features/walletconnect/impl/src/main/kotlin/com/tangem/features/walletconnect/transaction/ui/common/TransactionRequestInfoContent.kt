package com.tangem.features.walletconnect.transaction.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButtonIconEnd
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import kotlinx.collections.immutable.persistentListOf

private const val MIN_HEIGHT_SCREEN_PERCENT = 0.35f
private const val MAX_HEIGHT_SCREEN_PERCENT = 0.75f

@Composable
internal fun TransactionRequestInfoContent(
    state: WcTransactionRequestInfoUM,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val minHeight = (screenHeight * MIN_HEIGHT_SCREEN_PERCENT).dp
    val maxHeight = (screenHeight * MAX_HEIGHT_SCREEN_PERCENT).dp

    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        onBack = onBack,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.wc_transaction_request_title),
                startIconRes = R.drawable.ic_back_24,
                onStartClick = onBack,
            )
        },
        content = {
            TransactionRequestContent(state = state, minHeight = minHeight, maxHeight = maxHeight)
        },
        footer = {
            SecondaryButtonIconEnd(
                modifier = Modifier
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.wc_copy_data_button_text),
                onClick = state.onCopy,
                iconResId = R.drawable.ic_copy_24,
            )
        },
    )
}

@Composable
private fun TransactionRequestContent(state: WcTransactionRequestInfoUM, minHeight: Dp, maxHeight: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .padding(horizontal = 16.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 70.dp),
        ) {
            items(state.blocks) { block ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(TangemTheme.colors.background.action)
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp),
                ) {
                    block.info.forEach { item ->
                        Text(
                            text = item.title.resolveReference(),
                            style = TangemTheme.typography.subtitle2,
                            color = TangemTheme.colors.text.tertiary,
                        )
                        if (item.description.isNotEmpty()) {
                            Text(
                                text = item.description,
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                                style = TangemTheme.typography.body2,
                                color = TangemTheme.colors.text.primary1,
                            )
                        } else {
                            Spacer(modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcSignTransactionRequestInfoBottomSheetPreview(
    @PreviewParameter(WcSignTransactionStateProvider::class) state: WcTransactionRequestInfoUM,
) {
    TangemThemePreview {
        TangemModalBottomSheet<WcTransactionRequestInfoUM>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = state,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = {
                TangemModalBottomSheetTitle(
                    title = resourceReference(R.string.wc_transaction_request_title),
                    endIconRes = null,
                    onEndClick = {},
                    startIconRes = R.drawable.ic_back_24,
                    onStartClick = {},
                )
            },
            content = {
                TransactionRequestInfoContent(state, {}, {})
            },
        )
    }
}

private class WcSignTransactionStateProvider : CollectionPreviewParameterProvider<WcTransactionRequestInfoUM>(
    listOf(
        WcTransactionRequestInfoUM(
            blocks = persistentListOf(
                WcTransactionRequestBlockUM(
                    persistentListOf(
                        WcTransactionRequestInfoItemUM(
                            title = resourceReference(R.string.wc_signature_type),
                            description = "personal_sign",
                        ),
                        WcTransactionRequestInfoItemUM(
                            title = resourceReference(R.string.wc_contents),
                            description = "Hello! My name is John Dow. test@tange.com",
                        ),
                    ),
                ),
            ),
            onCopy = {},
        ),
        WcTransactionRequestInfoUM(
            blocks = persistentListOf(
                WcTransactionRequestBlockUM(
                    persistentListOf(
                        WcTransactionRequestInfoItemUM(
                            title = resourceReference(R.string.wc_signature_type),
                            description = "personal_sign",
                        ),
                        WcTransactionRequestInfoItemUM(
                            title = resourceReference(R.string.wc_contents),
                            description = "Hello! My name is John Dow. test@tange.com",
                        ),
                    ),
                ),
                WcTransactionRequestBlockUM(
                    persistentListOf(
                        WcTransactionRequestInfoItemUM(
                            title = resourceReference(R.string.wc_transaction_info_to_title),
                        ),
                        WcTransactionRequestInfoItemUM(
                            title = resourceReference(R.string.settings_wallet_name_title),
                            description = "Bob",
                        ),
                        WcTransactionRequestInfoItemUM(
                            title = resourceReference(R.string.wc_common_wallet),
                            description = "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826",
                        ),
                    ),
                ),
            ),
            onCopy = {},
        ),
    ),
)