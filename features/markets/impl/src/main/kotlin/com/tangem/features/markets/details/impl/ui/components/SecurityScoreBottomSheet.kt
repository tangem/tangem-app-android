package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetTitle
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.details.impl.ui.preview.SecurityScorePreviewData
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.details.impl.ui.state.SecurityScoreBottomSheetContent

@Composable
internal fun SecurityScoreBottomSheet(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheet<SecurityScoreBottomSheetContent>(
        config = config,
        addBottomInsets = false,
        title = { TangemBottomSheetTitle(title = it.title) },
        content = { content ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TangemTheme.dimens.spacing16),
            ) {
                Text(
                    text = content.description.resolveReference(),
                    style = TangemTheme.typography.body2.copy(
                        color = TangemTheme.colors.text.secondary,
                    ),
                )

                SpacerH12()
                content.providers.fastForEachIndexed { index, provider ->
                    DividerContainer(
                        modifier = Modifier
                            .roundedShapeItemDecoration(
                                currentIndex = index,
                                lastIndex = content.providers.lastIndex,
                                addDefaultPadding = false,
                            )
                            .background(TangemTheme.colors.background.action),
                        showDivider = index != content.providers.lastIndex,
                    ) {
                        SecurityScoreProviderRow(
                            providerUM = provider,
                            onLinkClick = content.onProviderLinkClick,
                        )
                    }
                }

                SpacerH16()
                SpacerH(bottomBarHeight)
            }
        },
    )
}

@Composable
private fun SecurityScoreProviderRow(
    providerUM: SecurityScoreBottomSheetContent.SecurityScoreProviderUM,
    onLinkClick: (SecurityScoreBottomSheetContent.SecurityScoreProviderUM) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing12)
            .heightIn(min = TangemTheme.dimens.size68),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = TangemTheme.dimens.size40)
                .clip(TangemTheme.shapes.roundedCorners8),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(providerUM.iconUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            error = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            contentDescription = null,
        )

        Column(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing12),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            Text(
                text = providerUM.name,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            providerUM.lastAuditDate?.let {
                Text(
                    text = providerUM.lastAuditDate,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }

        SpacerWMax()

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            modifier = Modifier.clickable(
                enabled = providerUM.urlData != null,
                indication = ripple(bounded = false),
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onLinkClick(providerUM) },
            ),
        ) {
            ScoreStarsBlock(
                score = providerUM.score,
                scoreTextStyle = TangemTheme.typography.body2,
                horizontalSpacing = TangemTheme.dimens.spacing3,
            )

            UrlBlock(providerUM)
        }
    }
}

@Composable
private fun UrlBlock(providerUM: SecurityScoreBottomSheetContent.SecurityScoreProviderUM) {
    val urlData = providerUM.urlData
    val rootHost = urlData?.rootHost
    if (urlData != null && rootHost != null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            Text(
                text = urlData.rootHost,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
            Icon(
                modifier = Modifier
                    .size(TangemTheme.dimens.size16),
                painter = painterResource(id = R.drawable.ic_arrow_top_right_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityScoreBottomSheetPreview() {
    TangemThemePreview {
        SecurityScoreBottomSheet(
            config = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = {},
                content = SecurityScorePreviewData.bottomSheetContent,
            ),
        )
    }
}