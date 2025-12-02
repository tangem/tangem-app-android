package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.R
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.inputrow.InputRowImageBase
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState

@Composable
internal fun TangemPayProgressState(state: TangemPayState.Progress, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary)
            .clickable(onClick = state.onButtonClick),
    ) {
        InputRowImageBase(
            modifier = Modifier
                .padding(
                    all = TangemTheme.dimens.spacing12,
                ),
            subtitle = state.title,
            caption = state.description,
            subtitleColor = TangemTheme.colors.text.primary1,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = R.drawable.img_visa_36,
        )
    }
}