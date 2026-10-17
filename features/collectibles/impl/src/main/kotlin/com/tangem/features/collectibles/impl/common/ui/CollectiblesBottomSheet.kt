package com.tangem.features.collectibles.impl.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun CollectiblesBottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = onDismiss) })
        },
        content = { content() },
    )
}

/**
 * Not [TangemModalStatus]: the design spaces the avatar further from the title and sets the
 * description in a larger style than the DS block does.
 *
 * @param icon null keeps the [variant]'s own glyph
 */
@Suppress("LongParameterList")
@Composable
internal fun CollectiblesBottomSheetContent(
    variant: TangemModalStatus.Variant,
    icon: ImageVector?,
    title: TextReference,
    description: TextReference,
    modifier: Modifier = Modifier,
    buttons: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (icon != null) {
                TangemModalStatus.Avatar(variant = variant, imageVector = icon)
            } else {
                TangemModalStatus.Avatar(variant = variant)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title.resolveReference(),
                    style = TangemTheme.typography3.heading.small,
                    color = TangemTheme.colors3.text.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = description.resolveReference(),
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        TangemFooter(
            windowInsets = WindowInsets(0),
            fadeEnabled = false,
            blurBackground = false,
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            TangemButtonGroup(content = buttons)
        }
    }
}

@Composable
internal fun CollectiblesBottomSheetPrimaryButton(text: TextReference, onClick: () -> Unit) {
    TangemButton(
        variant = TangemButton.Variant.Primary,
        size = TangemButton.Size.X12,
        text = text,
        onClick = onClick,
    )
}