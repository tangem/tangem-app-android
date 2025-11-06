package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetV2Content
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.TextReference.Res
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAddFundsItemUM
import com.tangem.features.tangempay.entity.TangemPayAddFundsUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayAddFundsContent(state: TangemPayAddFundsUM) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.dismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.dismiss,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.tangempay_card_details_add_funds),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = state.dismiss,
            )
        },
    ) {
        if (state.errorMessage != null) {
            MessageBottomSheetV2Content(state.errorMessage)
        } else {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = TangemTheme.shapes.roundedCornersXMedium,
                color = TangemTheme.colors.background.primary,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                        text = resourceReference(R.string.tangempay_card_details_add_funds_subtitle).resolveReference(),
                        color = TangemTheme.colors.text.tertiary,
                        style = TangemTheme.typography.subtitle2,
                    )
                    state.items.fastForEach {
                        key(it.title) {
                            TangemPayTopUpItem(state = it)
                        }
                    }
                }
            }
            SpacerH16()
        }
    }
}

@Composable
private fun TangemPayTopUpItem(state: TangemPayAddFundsItemUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = state.onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                )
                .size(36.dp),
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = ImageVector.vectorResource(id = state.iconRes),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = state.description.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayAddFundsContentPreview() {
    TangemThemePreview {
        TangemPayAddFundsContent(
            state = TangemPayAddFundsUM(
                items = persistentListOf(
                    TangemPayAddFundsItemUM(
                        iconRes = R.drawable.ic_exchange_vertical_24,
                        title = Res(R.string.common_exchange),
                        description = Res(R.string.exсhange_token_description),
                        onClick = {},
                    ),
                    TangemPayAddFundsItemUM(
                        iconRes = R.drawable.ic_arrow_down_24,
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