package com.tangem.features.createwalletstart.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButtonIconEnd
import com.tangem.core.ui.components.bottomFade
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.createwalletstart.entity.CreateWalletStartUM
import com.tangem.features.createwalletstart.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.max

@Suppress("LongMethod", "MagicNumber")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreateWalletStartContent(state: CreateWalletStartUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        TangemColorPalette.Dark6,
                        TangemColorPalette.Black,
                    ),
                ),
            )
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
            navigationIcon = {
                IconButton(onClick = state.onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = null,
                    )
                }
            },
            title = { },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .bottomFade(height = 24.dp),
        ) {
            AdaptiveScrollableContent(
                topContent = {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 32.dp,
                                top = 16.dp,
                                end = 32.dp,
                            ),
                        text = state.title.resolveReference(),
                        style = TangemTheme.typography.h2,
                        color = TangemTheme.colors.text.primary1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 32.dp,
                                top = 8.dp,
                                end = 32.dp,
                            ),
                        text = state.description.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.primary1,
                        textAlign = TextAlign.Center,
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 24.dp,
                                top = 16.dp,
                                end = 24.dp,
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.featureItems.forEach { item ->
                            FeatureItem(
                                iconResId = item.iconResId,
                                text = item.text,
                            )
                        }
                    }
                },
                imageContent = {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(
                                vertical = 12.dp,
                                horizontal = 16.dp,
                            ),
                        painter = painterResource(id = state.imageResId),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                    )
                },
                bottomContent = {
                    if (state.shouldShowScanSecondaryButton) {
                        SecondaryButtonIconEnd(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            text = stringResourceSafe(R.string.welcome_unlock_card),
                            onClick = state.onScanClick,
                            showProgress = state.isScanInProgress,
                            iconResId = R.drawable.ic_tangem_24,
                        )
                    }
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp,
                            ),
                        text = state.primaryButtonText.resolveReference(),
                        onClick = state.onPrimaryButtonClick,
                    )

                    Row(
                        modifier = Modifier
                            .padding(
                                start = 16.dp,
                                top = 24.dp,
                                end = 16.dp,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DashedGradientLine(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp),
                        )
                        Text(
                            text = stringResourceSafe(R.string.common_or),
                            style = TangemTheme.typography.caption1,
                            color = TangemTheme.colors.text.secondary,
                            textAlign = TextAlign.Center,
                        )
                        DashedGradientLine(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .scale(scaleX = -1f, scaleY = 1f),
                        )
                    }

                    if (state.otherMethodDescription != null) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    top = 16.dp,
                                    end = 16.dp,
                                ),
                            text = state.otherMethodDescription.resolveReference(),
                            style = TangemTheme.typography.subtitle2,
                            color = TangemTheme.colors.text.tertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .clickable { state.otherMethodClick() }
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp,
                            ),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.otherMethodTitle.resolveReference(),
                            style = TangemTheme.typography.subtitle1,
                            color = TangemTheme.colors.text.primary1,
                            textAlign = TextAlign.Center,
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right_18x24),
                            tint = TangemTheme.colors.icon.primary1,
                            contentDescription = null,
                        )
                    }
                },
                minImageHeight = 160.dp,
            )
        }
        if (!state.shouldShowScanSecondaryButton) {
            FlowRow(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(
                        start = 16.dp,
                        top = 24.dp,
                        end = 16.dp,
                        bottom = 8.dp,
                    ),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResourceSafe(R.string.welcome_create_wallet_already_have),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.secondary,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { state.onScanClick() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResourceSafe(R.string.wallet_create_scan_title),
                        style = TangemTheme.typography.caption1,
                        color = TangemTheme.colors.text.primary1,
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_tangem_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = null,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AdaptiveScrollableContent(
    minImageHeight: Dp,
    modifier: Modifier = Modifier,
    topContent: @Composable () -> Unit,
    imageContent: @Composable () -> Unit,
    bottomContent: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val viewportHeight = maxHeight
        val minImageHeightPx = with(density) { minImageHeight.roundToPx() }
        val viewportHeightPx = with(density) { viewportHeight.roundToPx() }
        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(initial = Int.MAX_VALUE)),
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    topContent()
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    imageContent()
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    bottomContent()
                }
            },
        ) { measurables, constraints ->
            val topPlaceable = measurables[0].measure(
                constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity),
            )
            val bottomPlaceable = measurables[2].measure(
                constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity),
            )
            val imageIntrinsicHeight = measurables[1].maxIntrinsicHeight(constraints.maxWidth)
            val availableHeightForImage = max(0, viewportHeightPx - topPlaceable.height - bottomPlaceable.height)
            val targetImageHeight = when {
                imageIntrinsicHeight < minImageHeightPx -> minImageHeightPx
                imageIntrinsicHeight > availableHeightForImage -> max(minImageHeightPx, availableHeightForImage)
                else -> imageIntrinsicHeight
            }
            val imagePlaceable = measurables[1].measure(
                constraints.copy(
                    minHeight = targetImageHeight,
                    maxHeight = targetImageHeight,
                ),
            )
            val totalContentHeight = topPlaceable.height + imagePlaceable.height + bottomPlaceable.height
            layout(constraints.maxWidth, totalContentHeight) {
                var yOffset = 0
                topPlaceable.placeRelative(0, yOffset)
                yOffset += topPlaceable.height
                imagePlaceable.placeRelative(0, yOffset)
                yOffset += imagePlaceable.height
                bottomPlaceable.placeRelative(0, yOffset)
            }
        }
    }
}

@Composable
private fun DashedGradientLine(modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    val strokeColor = TangemTheme.colors.stroke.primary

    Canvas(modifier = modifier) {
        val strokePx = with(density) { 4.dp.toPx() }
        val dashPx = with(density) { 4.dp.toPx() }
        val gapPx = with(density) { 8.dp.toPx() }

        val width = size.width
        val centerY = size.height / 2

        val brush = Brush.linearGradient(
            colors = listOf(strokeColor.copy(alpha = 0f), strokeColor),
            start = Offset(0f, 0f),
            end = Offset(width, 0f),
        )

        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)

        drawLine(
            brush = brush,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = strokePx,
            pathEffect = pathEffect,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun FeatureItem(@DrawableRes iconResId: Int, text: TextReference) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(iconResId),
            tint = TangemTheme.colors.icon.accent,
            contentDescription = null,
        )
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

private class CreateWalletStartStateProvider : CollectionPreviewParameterProvider<CreateWalletStartUM>(
    collection = listOf(
        CreateWalletStartUM(
            title = resourceReference(R.string.common_tangem_wallet),
            description = resourceReference(R.string.welcome_create_wallet_hardware_description),
            featureItems = persistentListOf(
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_shield_check_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_class),
                ),
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_flash_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_delivery),
                ),
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_sparkles_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_use),
                ),
            ),
            imageResId = R.drawable.img_hardware_wallet,
            shouldShowScanSecondaryButton = true,
            onPrimaryButtonClick = { },
            primaryButtonText = resourceReference(R.string.details_buy_wallet),
            otherMethodTitle = resourceReference(R.string.welcome_create_wallet_mobile_title),
            otherMethodDescription = null,
            otherMethodClick = { },
            onBackClick = { },
            onScanClick = { },
            isScanInProgress = false,
        ),
        CreateWalletStartUM(
            title = resourceReference(R.string.hw_mobile_wallet),
            description = resourceReference(R.string.welcome_create_wallet_mobile_description_full),
            featureItems = persistentListOf(
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_shield_check_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_seamless),
                ),
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_flash_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_one_tap),
                ),
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_stack_fill_new_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_assets),
                ),
            ),
            imageResId = R.drawable.img_mobile_wallet,
            shouldShowScanSecondaryButton = false,
            onPrimaryButtonClick = { },
            primaryButtonText = resourceReference(R.string.welcome_create_wallet_mobile_title),
            otherMethodTitle = resourceReference(R.string.welcome_create_wallet_use_hardware_title),
            otherMethodDescription = resourceReference(
                R.string.welcome_create_wallet_use_hardware_description,
            ),
            otherMethodClick = { },
            onBackClick = { },
            onScanClick = { },
            isScanInProgress = false,
        ),
    ),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 360, heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 360, heightDp = 840, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCreateWalletStartContent(
    @PreviewParameter(CreateWalletStartStateProvider::class) param: CreateWalletStartUM,
) {
    TangemThemePreview {
        CreateWalletStartContent(
            state = param,
        )
    }
}