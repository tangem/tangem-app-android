package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.rows.SimpleActionRow
import com.tangem.core.ui.components.rows.states.ActionRowState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.states.FeeItemState

@Composable
fun FeeItem(state: FeeItemState) {
    Box(
        modifier = Modifier
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .clickable(
                onClick = state.actionRowState.onClick,
            )
            .fillMaxWidth()
            .defaultMinSize(minHeight = TangemTheme.dimens.size68),
    ) {
        SimpleActionRow(
            modifier = Modifier.padding(
                start = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing12,
            ),
            state = state.actionRowState,
        )
    }
}

@Preview
@Composable
private fun FeeItemPreview() {
    val state = FeeItemState(
        id = "id",
        actionRowState = ActionRowState(
            title = "Title",
            description = "Description",
            onClick = {},
        ),
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