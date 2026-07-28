package com.tangem.core.ui.components.bottomsheets.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MessageBottomSheet(state: MessageBottomSheetUM, onDismissRequest: () -> Unit) {
    MessageBottomSheetV2(state, onDismissRequest)
}

@Composable
fun MessageBottomSheetContent(state: MessageBottomSheetUM, modifier: Modifier = Modifier) {
    MessageBottomSheetContentV2(state, modifier)
}