package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButtonIconEnd
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM

private const val MIN_HEIGHT_SCREEN_PERCENT = 0.35f
private const val MAX_HEIGHT_SCREEN_PERCENT = 0.75f

@Composable
internal fun WcTransactionRequestInfoContent(info: WcTransactionRequestInfoUM, actions: WcTransactionActionsUM) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val minHeight = (screenHeight * MIN_HEIGHT_SCREEN_PERCENT).dp
    val maxHeight = (screenHeight * MAX_HEIGHT_SCREEN_PERCENT).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = TangemTheme.dimens.spacing8, bottom = TangemTheme.dimens.spacing70),
        ) {
            items(info.blocks) { block ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                        .background(TangemTheme.colors.background.action)
                        .padding(
                            start = TangemTheme.dimens.spacing16,
                            end = TangemTheme.dimens.spacing16,
                            top = TangemTheme.dimens.spacing20,
                        ),
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
                                modifier = Modifier.padding(
                                    top = TangemTheme.dimens.spacing4,
                                    bottom = TangemTheme.dimens.spacing20,
                                ),
                                style = TangemTheme.typography.body2,
                                color = TangemTheme.colors.text.primary1,
                            )
                        } else {
                            Spacer(modifier = Modifier.size(TangemTheme.dimens.size12))
                        }
                    }
                }
                Spacer(modifier = Modifier.size(TangemTheme.dimens.size20))
            }
        }

        SecondaryButtonIconEnd(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = TangemTheme.dimens.spacing20)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.wc_copy_data_button_text),
            onClick = actions.onCopy,
            iconResId = R.drawable.ic_copy_24,
        )
    }
}