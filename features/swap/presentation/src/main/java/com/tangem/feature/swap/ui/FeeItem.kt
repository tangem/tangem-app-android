package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.rows.SimpleActionRow
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.domain.models.ui.FeeType
import com.tangem.feature.swap.models.states.FeeItemState

@Composable
fun FeeItemBlock(state: FeeItemState) {
    if (state is FeeItemState.Content) {
        FeeItem(state = state)
    }
}

@Composable
fun FeeItem(state: FeeItemState.Content) {
    Box(
        modifier = Modifier
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .clickable(
                onClick = state.onClick,
            )
            .fillMaxWidth()
            .defaultMinSize(minHeight = TangemTheme.dimens.size68),
    ) {
        val description = "${state.amountCrypto} ${state.symbolCrypto} (${state.amountFiatFormatted})"
        SimpleActionRow(
            modifier = Modifier.padding(
                start = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing12,
            ),
            title = state.title.resolveReference(),
            description = description,
            isClickable = state.isClickable,
        )
    }
}

@Preview
@Composable
private fun FeeItemPreview() {
    val state = FeeItemState.Content(
        feeType = FeeType.NORMAL,
        title = stringReference("Fee"),
        amountCrypto = "1000",
        symbolCrypto = "MATIC",
        amountFiatFormatted = "(1000$)",
        isClickable = false,
        onClick = {},
    )
    Column {
        TangemThemePreview(isDark = false) {
            FeeItem(state = state)
        }

        SpacerH24()

        TangemThemePreview(isDark = true) {
            FeeItem(state = state)
        }
    }
}
