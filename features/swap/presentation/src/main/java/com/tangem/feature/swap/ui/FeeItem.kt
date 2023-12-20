package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.rows.SimpleActionRow
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
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
    Column(
        modifier = Modifier
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(start = TangemTheme.dimens.spacing12)
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
                top = TangemTheme.dimens.spacing12,
            ),
            title = state.title.resolveReference(),
            description = description,
            isClickable = state.isClickable,
        )
        state.explanation?.let {
            Divider(
                color = TangemTheme.colors.stroke.primary,
                thickness = TangemTheme.dimens.size0_5,
                modifier = Modifier.padding(
                    top = TangemTheme.dimens.spacing10,
                    bottom = TangemTheme.dimens.spacing10,
                    end = TangemTheme.dimens.spacing2,
                ),
            )
            Text(
                text = it.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.padding(
                    bottom = TangemTheme.dimens.spacing10,
                    end = TangemTheme.dimens.spacing16,
                ),
            )
        }
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
        explanation = stringReference(
            "Additionally, the network fee for sending the exchanged funds back to your address is " +
                "included in the rate",
        ),
        isClickable = false,
        onClick = {},
    )
    Column {
        TangemTheme(isDark = false) {
            FeeItem(state = state)
        }

        SpacerH24()

        TangemTheme(isDark = true) {
            FeeItem(state = state)
        }
    }
}