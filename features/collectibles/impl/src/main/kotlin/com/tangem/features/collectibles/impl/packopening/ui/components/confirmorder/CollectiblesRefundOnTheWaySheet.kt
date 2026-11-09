package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

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
internal fun CollectiblesRefundOnTheWaySheet(onCloseClick: () -> Unit, onDismiss: () -> Unit) {
    CollectiblesBottomSheet(onDismiss = onDismiss) {
        RefundOnTheWayContent(onCloseClick = onCloseClick)
    }
}

@Composable
private fun RefundOnTheWayContent(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiblesBottomSheetContent(
        variant = TangemModalStatus.Variant.Info,
        icon = null,
        title = resourceReference(R.string.collectibles_refund_on_the_way_title),
        description = resourceReference(R.string.collectibles_refund_on_the_way_subtitle),
        modifier = modifier,
    ) {
        CollectiblesBottomSheetPrimaryButton(
            text = resourceReference(R.string.common_close),
            onClick = onCloseClick,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesRefundOnTheWaySheet_Preview() {
    TangemThemePreviewRedesign {
        RefundOnTheWayContent(onCloseClick = {}, modifier = Modifier.background(TangemTheme.colors3.bg.primary))
    }
}