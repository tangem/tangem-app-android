package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.preview.FeeItemStatePreview

@Composable
fun FeeItemBlock(state: FeeItemState) {
    if (state is FeeItemState.Content) {
        FeeItem(state = state)
    }
}

@Composable
fun FeeItem(state: FeeItemState.Content) {
    val description = "${state.amountCrypto} ${state.symbolCrypto} (${state.amountFiatFormatted})"
    val icon = R.drawable.ic_chevron_right_24.takeIf { state.isClickable }
    InputRowDefault(
        title = state.title,
        text = stringReference(description),
        iconRes = icon,
        modifier = Modifier
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .background(color = TangemTheme.colors.background.action)
            .clickable(
                enabled = state.isClickable,
                onClick = state.onClick,
            ),
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeeItem_Preview(@PreviewParameter(FeeItemPreviewProvider::class) data: FeeItemState.Content) {
    TangemThemePreview {
        FeeItem(data)
    }
}

private class FeeItemPreviewProvider : PreviewParameterProvider<FeeItemState.Content> {
    override val values: Sequence<FeeItemState.Content>
        get() = sequenceOf(
            FeeItemStatePreview.state,
            FeeItemStatePreview.state.copy(isClickable = true),
        )
}
// endregion
