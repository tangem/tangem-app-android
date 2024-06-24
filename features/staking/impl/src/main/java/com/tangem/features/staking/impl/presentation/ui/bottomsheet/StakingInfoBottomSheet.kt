package com.tangem.features.staking.impl.presentation.ui.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.appbar.AppBar
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.presentation.state.bottomsheet.StakingInfoBottomSheetConfig

@Composable
fun StakingInfoBottomSheet(config: TangemBottomSheetConfig) {
    val scrollState = rememberScrollState()
    TangemBottomSheet(
        config = config,
    ) { content: StakingInfoBottomSheetConfig ->
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            AppBar(text = content.title)
            Text(
                text = content.text.resolveReference(),
                color = TangemTheme.colors.text.secondary,
                style = TangemTheme.typography.body2,
                modifier = Modifier.padding(
                    horizontal = TangemTheme.dimens.spacing28,
                    vertical = TangemTheme.dimens.spacing16,
                ),
            )
        }
    }
}