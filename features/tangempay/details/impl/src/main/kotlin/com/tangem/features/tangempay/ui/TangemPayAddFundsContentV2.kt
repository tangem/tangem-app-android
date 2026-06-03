package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetContent
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference.Res
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.LocalVisaRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAddFundsItemUM
import com.tangem.features.tangempay.entity.TangemPayAddFundsUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayAddFundsContentV2(state: TangemPayAddFundsUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.dismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.dismiss,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                title = resourceReference(R.string.tangempay_card_details_add_funds),
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = state.dismiss,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = {
            if (state.errorMessage != null) {
                MessageBottomSheetContent(state.errorMessage)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3),
                ) {
                    state.items.fastForEach { item ->
                        key(item.title) {
                            TangemPayTopUpItem(state = item)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun TangemPayTopUpItem(state: TangemPayAddFundsItemUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = state.onClick)
            .padding(vertical = TangemTheme.dimens2.x3),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    color = TangemTheme.colors3.bg.status.infoSubtle,
                    shape = CircleShape,
                )
                .size(TangemTheme.dimens2.x10),
        ) {
            TangemIcon(
                modifier = Modifier.size(TangemTheme.dimens2.x5),
                tangemIconUM = state.icon,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x0_5)) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = state.description.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayAddFundsContentPreview() {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(
            LocalVisaRedesignEnabled provides true,
            LocalRedesignEnabled provides true,
        ) {
            TangemPayAddFundsContentV2(
                state = TangemPayAddFundsUM(
                    items = persistentListOf(
                        TangemPayAddFundsItemUM(
                            icon = TangemIconUM.Icon(
                                imageVector = Icons.ic_logo_tangem_20,
                                tintReference = {
                                    TangemTheme.colors3.icon.brand
                                },
                            ),
                            title = Res(R.string.common_exchange),
                            description = Res(R.string.exсhange_token_description),
                            onClick = {},
                        ),
                        TangemPayAddFundsItemUM(
                            icon = TangemIconUM.Icon(
                                imageVector = Icons.ic_card_20,
                                tintReference = {
                                    TangemTheme.colors3.icon.brand
                                },
                            ),
                            title = Res(R.string.common_receive),
                            description = Res(R.string.receive_token_description),
                            onClick = {},
                        ),
                    ),
                    dismiss = {},
                    errorMessage = null,
                ),
            )
        }
    }
}