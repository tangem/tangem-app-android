package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.states.ChooseFeeBottomSheetConfig

@Composable
fun ChooseFeeBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(config) { content: ChooseFeeBottomSheetConfig ->
        ChooseFeeBottomSheetContent(content = content)
    }
}

@Suppress("UnusedPrivateMember")
@Composable
private fun ChooseFeeBottomSheetContent(content: ChooseFeeBottomSheetConfig) {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
    ) {
    }
}