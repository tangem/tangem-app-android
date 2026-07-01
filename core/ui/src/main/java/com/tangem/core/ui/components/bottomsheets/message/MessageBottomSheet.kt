package com.tangem.core.ui.components.bottomsheets.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.LocalRedesignEnabled

@Composable
fun MessageBottomSheet(state: MessageBottomSheetUM, onDismissRequest: () -> Unit) {
    if (LocalRedesignEnabled.current) {
        MessageBottomSheetV2(state, onDismissRequest)
    } else {
        MessageBottomSheetV1(state, onDismissRequest)
    }
}

@Composable
fun MessageBottomSheetContent(state: MessageBottomSheetUM, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        MessageBottomSheetContentV2(state, modifier)
    } else {
        MessageBottomSheetContentV1(state, modifier)
    }
}