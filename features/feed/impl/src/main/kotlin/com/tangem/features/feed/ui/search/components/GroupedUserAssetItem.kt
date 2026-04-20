package com.tangem.features.feed.ui.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.components.LayeringIcons
import com.tangem.features.feed.ui.search.state.UserAssetItemUM

@Composable
internal fun GroupedUserAssetItem(item: UserAssetItemUM.Grouped, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors2.surface.level3,
            shape = RoundedCornerShape(TangemTheme.dimens2.x5),
        ),
    ) {
        TangemRowContainer(
            modifier = Modifier.clickable(onClick = item.onClick),
            content = {
                LayeringIcons(
                    modifier = Modifier
                        .layoutId(layoutId = TangemRowLayoutId.HEAD)
                        .padding(end = TangemTheme.dimens2.x1),
                    tangemIconUM = item.icon,
                    count = item.tokensCount,
                )

                Text(
                    modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
                    text = item.tokenName,
                    style = TangemTheme.typography2.bodyMedium16,
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )

                Text(
                    modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
                    text = pluralStringResourceSafe(R.plurals.common_tokens_count, item.tokensCount, item.tokensCount),
                    style = TangemTheme.typography2.captionMedium12,
                    color = TangemTheme.colors2.text.neutral.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                BalanceColumn(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_TOP),
                    balanceState = item.balanceState,
                    isBalanceHidden = item.isBalanceHidden,
                )

                TangemButton(
                    modifier = Modifier
                        .padding(start = TangemTheme.dimens2.x2)
                        .layoutId(TangemRowLayoutId.TAIL),
                    buttonUM = TangemButtonUM(
                        type = TangemButtonType.Secondary,
                        tangemIconUM = TangemIconUM.Icon(
                            iconRes = R.drawable.ic_chevron_24,
                            tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                        ),
                        shape = TangemButtonShape.Rounded,
                        size = TangemButtonSize.X10,
                        onClick = item.onClick,
                    ),
                )
            },
        )
    }
}