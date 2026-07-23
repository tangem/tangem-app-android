package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.state.InfoBottomSheetContent
import dev.jeziellago.compose.markdowntext.MarkdownText

@Suppress("LongMethod")
@Composable
internal fun InfoBottomSheet(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheet<InfoBottomSheetContent>(
        config = config,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = { content ->
            TangemTopNavigation(
                title = content.title,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                blurBackground = false,
                onClose = config.onDismissRequest,
            )
        },
        content = { content ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                MarkdownText(
                    modifier = Modifier.padding(bottom = 12.dp),
                    markdown = content.body.resolveReference(),
                    disableLinkMovementMethod = true,
                    linkifyMask = 0,
                    syntaxHighlightColor = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.body.medium.copy(
                        TangemTheme.colors3.text.secondary,
                    ),
                )

                if (content.generatedAINotificationUM != null) {
                    TangemRowContainer(
                        modifier = Modifier
                            .background(
                                color = TangemTheme.colors3.bg.tertiary,
                                shape = RoundedCornerShape(20.dp),
                            )
                            .clickableSingle(onClick = content.generatedAINotificationUM.onClick),
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp)
                                .layoutId(TangemRowLayoutId.HEAD),
                            tint = TangemTheme.colors3.icon.brand,
                            contentDescription = null,
                            imageVector = ImageVector.vectorResource(R.drawable.ic_magic_28),
                        )

                        Text(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .layoutId(TangemRowLayoutId.START_TOP),
                            text = stringResourceSafe(R.string.information_generated_with_ai),
                            style = TangemTheme.typography3.caption.medium,
                            color = TangemTheme.colors3.text.primary,
                        )
                    }
                }
                SpacerH(bottomBarHeight)
            }
        },
    )
}