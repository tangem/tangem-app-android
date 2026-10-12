package com.tangem.features.collectibles.impl.confirmorder.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheet
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheetContent
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheetPrimaryButton

@Composable
internal fun CollectiblesLowInventorySheet(
    onEnableTurboModeClick: () -> Unit,
    onExplorePacksClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    CollectiblesBottomSheet(onDismiss = onDismiss) {
        LowInventoryContent(onEnableTurboModeClick = onEnableTurboModeClick, onExplorePacksClick = onExplorePacksClick)
    }
}

@Composable
private fun LowInventoryContent(
    onEnableTurboModeClick: () -> Unit,
    onExplorePacksClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiblesBottomSheetContent(
        variant = TangemModalStatus.Variant.Error,
        icon = Icons.ic_error_28,
        title = resourceReference(R.string.collectibles_low_inventory_title),
        description = resourceReference(R.string.collectibles_low_inventory_subtitle),
        modifier = modifier,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.collectibles_low_inventory_enable_turbo_mode),
            onClick = onEnableTurboModeClick,
        )
        CollectiblesBottomSheetPrimaryButton(
            text = resourceReference(R.string.collectibles_low_inventory_explore_packs),
            onClick = onExplorePacksClick,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesLowInventorySheet_Preview() {
    TangemThemePreviewRedesign {
        LowInventoryContent(
            onEnableTurboModeClick = {},
            onExplorePacksClick = {},
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}