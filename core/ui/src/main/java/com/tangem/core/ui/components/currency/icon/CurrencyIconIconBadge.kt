package com.tangem.core.ui.components.currency.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_16

@Composable
internal fun CurrencyIconBottomBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size12)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = CircleShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(all = TangemTheme.dimens.spacing2)
                .matchParentSize()
                .background(
                    color = TangemTheme.colors.icon.informative,
                    shape = CircleShape,
                ),
        )
    }
}

/** A small circular badge with a clock icon, e.g. for an account with a pending co-sign operation. */
@Composable
internal fun CurrencyIconPendingOperationBadge(modifier: Modifier = Modifier) {
    val dimens = TangemTheme.dimens
    val contentSize = dimens.size10 + dimens.spacing3 * 2

    Box(
        modifier = modifier
            .size(contentSize + dimens.spacing2 * 2)
            .background(
                color = TangemTheme.colors3.bg.secondary,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(contentSize)
                .background(
                    color = TangemTheme.colors3.bg.status.warningSubtle,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_clock_16,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.warning,
                modifier = Modifier.size(dimens.size10),
            )
        }
    }
}