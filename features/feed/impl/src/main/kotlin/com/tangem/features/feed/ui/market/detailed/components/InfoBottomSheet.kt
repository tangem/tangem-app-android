package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.state.InfoBottomSheetContent
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet as TangemBottomSheetV2

@Composable
internal fun InfoBottomSheet(config: TangemBottomSheetConfig) {
    InfoBottomSheetV2(config)
}

@Suppress("LongMethod")
@Composable
private fun InfoBottomSheetV2(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheetV2<InfoBottomSheetContent>(
        config = config,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors2.surface.level3,
        title = { content ->
            TangemTopBar(
                title = content.title,
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_close_24),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .background(
                                color = TangemTheme.colors2.button.backgroundSecondary,
                                shape = CircleShape,
                            )
                            .clickableSingle(onClick = config.onDismissRequest)
                            .padding(TangemTheme.dimens2.x2_5),
                    )
                },
            )
        },
        content = { content ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TangemTheme.dimens2.x4),
            ) {
                MarkdownText(
                    modifier = Modifier.padding(bottom = TangemTheme.dimens2.x3),
                    markdown = content.body.resolveReference(),
                    disableLinkMovementMethod = true,
                    linkifyMask = 0,
                    syntaxHighlightColor = TangemTheme.colors2.text.neutral.secondary,
                    style = TangemTheme.typography2.bodyRegular16.copy(
                        TangemTheme.colors2.text.neutral.secondary,
                    ),
                )

                if (content.generatedAINotificationUM != null) {
                    TangemRowContainer(
                        modifier = Modifier
                            .background(
                                color = TangemTheme.colors2.surface.level4,
                                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
                            )
                            .clickableSingle(onClick = content.generatedAINotificationUM.onClick),
                        contentPadding = PaddingValues(TangemTheme.dimens2.x3),
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(TangemTheme.dimens2.x10)
                                .padding(TangemTheme.dimens2.x1)
                                .layoutId(TangemRowLayoutId.HEAD),
                            tint = TangemTheme.colors2.markers.iconBlue,
                            contentDescription = null,
                            imageVector = ImageVector.vectorResource(R.drawable.ic_magic_28),
                        )

                        Text(
                            modifier = Modifier
                                .padding(start = TangemTheme.dimens2.x1)
                                .layoutId(TangemRowLayoutId.START_TOP),
                            text = stringResourceSafe(R.string.information_generated_with_ai),
                            style = TangemTheme.typography2.captionSemibold12,
                            color = TangemTheme.colors2.text.neutral.primary,
                        )
                    }
                }
                SpacerH(bottomBarHeight)
            }
        },
    )
}