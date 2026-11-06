package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
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

// TODO([REDACTED_TASK_KEY]): localize the title and the description
@Composable
private fun RefundOnTheWayContent(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiblesBottomSheetContent(
        variant = TangemModalStatus.Variant.Info,
        icon = null,
        title = stringReference("Your refund is on the way"),
        description = stringReference(
            "Our partner should have issued refund for your unopened pack. " +
                "If you haven’t received a refund within a day, please contact support",
        ),
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