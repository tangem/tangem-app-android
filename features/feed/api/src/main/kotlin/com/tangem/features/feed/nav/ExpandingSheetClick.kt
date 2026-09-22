package com.tangem.features.feed.nav

import androidx.compose.runtime.State
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion

fun expandingSheetClick(
    sheetExpansion: State<ShtorkaExpansion>,
    onExpandSheet: () -> Unit,
    onClick: () -> Unit,
): () -> Unit = {
    onClick()

    if (sheetExpansion.value == ShtorkaExpansion.PILL) {
        onExpandSheet()
    }
}