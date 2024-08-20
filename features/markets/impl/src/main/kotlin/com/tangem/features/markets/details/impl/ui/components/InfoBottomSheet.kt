package com.tangem.features.markets.details.impl.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetTitle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent

@Composable
internal fun InfoBottomSheet(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheet<InfoBottomSheetContent>(
        config = config,
        skipPartiallyExpanded = false,
        addBottomInsets = false,
        title = {
            TangemBottomSheetTitle(title = it.title)
        },
        content = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TangemTheme.dimens.spacing28),
            ) {
                Text(
                    text = it.body.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
                SpacerH(bottomBarHeight)
            }
        },
    )
}