package com.tangem.features.collectibles.impl.machinedetails.turboinfo

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheet
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheetContent
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheetPrimaryButton

@Composable
internal fun CollectiblesTurboInfoBottomSheet(onDismiss: () -> Unit) {
    CollectiblesBottomSheet(onDismiss = onDismiss) {
        TurboInfoContent(onGotItClick = onDismiss)
    }
}

@Composable
private fun TurboInfoContent(onGotItClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiblesBottomSheetContent(
        variant = TangemModalStatus.Variant.Info,
        icon = null,
        title = resourceReference(R.string.collectibles_turbo_mode_bottomsheet_title),
        description = resourceReference(R.string.collectibles_turbo_mode_bottomsheet_message),
        modifier = modifier,
    ) {
        CollectiblesBottomSheetPrimaryButton(text = resourceReference(R.string.common_got_it), onClick = onGotItClick)
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesTurboInfoBottomSheet_Preview() {
    TangemThemePreviewRedesign {
        TurboInfoContent(onGotItClick = {}, modifier = Modifier.background(TangemTheme.colors3.bg.primary))
    }
}