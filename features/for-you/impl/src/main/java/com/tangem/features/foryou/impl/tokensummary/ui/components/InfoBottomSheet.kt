package com.tangem.features.foryou.impl.tokensummary.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation.ContentAlign
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.foryou.impl.tokensummary.entity.InfoBottomSheetContent

/**
 * Informational modal bottom sheet for the token summary screen.
 *
 * Renders the [infoBottomSheetContent]: a [title][InfoBottomSheetContent.title] with a trailing close (`✕`) button in the top
 * navigation, and a scrollable explanatory [body][InfoBottomSheetContent.body]. Visibility is driven by the hosting
 * Decompose slot, so the config is always shown; [onDismiss] delegates back to the slot navigation.
 *
 * @param infoBottomSheetContent the info content to display.
 * @param onDismiss invoked when the sheet is dismissed.
 */
@Composable
internal fun InfoBottomSheet(infoBottomSheetContent: InfoBottomSheetContent, onDismiss: () -> Unit) {
    TangemBottomSheet<InfoBottomSheetContent>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = infoBottomSheetContent,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.primary,
        title = { content ->
            TangemTopNavigation(
                windowInsets = WindowInsets(0),
                blurBackground = false,
                contentAlign = ContentAlign.Center,
                endButton = { TangemButton.Close(onClick = onDismiss) },
                contentColumn = {
                    Text(
                        text = content.title.resolveReference(),
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.body.medium,
                    )
                },
            )
        },
        content = { content ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(all = 16.dp),
            ) {
                Text(
                    text = content.body.resolveReference(),
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.subheading.medium,
                )
            }
        },
    )
}