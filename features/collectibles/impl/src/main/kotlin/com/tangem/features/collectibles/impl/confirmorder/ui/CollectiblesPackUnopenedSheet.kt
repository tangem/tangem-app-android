package com.tangem.features.collectibles.impl.confirmorder.ui

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
internal fun CollectiblesPackUnopenedSheet(onOpenPackClick: () -> Unit, onDismiss: () -> Unit) {
    CollectiblesBottomSheet(onDismiss = onDismiss) {
        PackUnopenedContent(onOpenPackClick = onOpenPackClick)
    }
}

@Composable
private fun PackUnopenedContent(onOpenPackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiblesBottomSheetContent(
        variant = TangemModalStatus.Variant.Warning,
        icon = null,
        title = resourceReference(R.string.collectibles_pack_unopened_title),
        description = resourceReference(R.string.collectibles_pack_unopened_subtitle),
        modifier = modifier,
    ) {
        CollectiblesBottomSheetPrimaryButton(
            text = resourceReference(R.string.collectibles_pack_unopened_open_pack),
            onClick = onOpenPackClick,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesPackUnopenedSheet_Preview() {
    TangemThemePreviewRedesign {
        PackUnopenedContent(onOpenPackClick = {}, modifier = Modifier.background(TangemTheme.colors3.bg.primary))
    }
}