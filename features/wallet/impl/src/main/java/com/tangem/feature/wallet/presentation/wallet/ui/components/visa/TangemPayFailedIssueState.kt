package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.inputrow.InputRowImageBase
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState

@Composable
internal fun TangemPayFailedIssueState(state: TangemPayState.FailedIssue) {
    BlockCard(
        modifier = Modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary),
    ) {
        InputRowImageBase(
            modifier = Modifier
                .padding(
                    all = TangemTheme.dimens.spacing12,
                )
                .clickable(onClick = { state.onButtonClick }),
            subtitle = state.title,
            caption = state.description,
            subtitleColor = TangemTheme.colors.text.primary1,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = com.tangem.core.ui.R.drawable.img_visa_36,
            iconEndRes = state.iconRes,
            endIconTint = TangemTheme.colors.icon.warning,
        )
    }
}