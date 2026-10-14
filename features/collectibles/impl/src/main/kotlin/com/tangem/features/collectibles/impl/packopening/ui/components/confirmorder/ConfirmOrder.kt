package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.common.ui.CollectiblesPackImage
import com.tangem.features.collectibles.impl.common.ui.CollectiblesPackImageErrorCard
import com.tangem.features.collectibles.impl.common.ui.CollectiblesStatus
import com.tangem.features.collectibles.impl.common.ui.PACK_IMAGE_CARD_RADIUS
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.OrderUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.PriceRowUM
import kotlinx.collections.immutable.ImmutableList

/** Pack size on the confirm screen (Figma `1607:41492`, 230 x 332 on a 402 frame). */
private const val PACK_WIDTH_FRACTION = 230f / 402f
private const val PACK_ASPECT = 332f / 230f

/** Least space kept above and below the pack; the rest is split evenly. */
private val PACK_MIN_TOP_GAP = 16.dp
private val PACK_MIN_BOTTOM_GAP = 44.dp

@Composable
internal fun ConfirmOrder(
    state: CollectiblesPackOpeningUM,
    modifier: Modifier = Modifier,
    progress: ConfirmProgress = ConfirmProgress.Idle,
) {
    TangemTopBarScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ConfirmOrderTopBar(state = state, exit = progress.exit) },
    ) { contentPadding ->
        when (state) {
            is CollectiblesPackOpeningUM.Loading -> ConfirmOrderSkeleton(contentPadding = contentPadding)
            is CollectiblesPackOpeningUM.Error -> ConfirmOrderError(state = state, contentPadding = contentPadding)
            is CollectiblesPackOpeningUM.Content -> ConfirmOrderLayout(
                contentPadding = contentPadding,
                pack = { packModifier ->
                    PackImage(imageUrl = state.order.packImageUrl, progress = progress, modifier = packModifier)
                },
                rows = {
                    PriceRows(
                        rows = state.order.rows,
                        modifier = Modifier.dropAway(exit = progress.exit, isDelayed = true),
                    )
                },
                footer = {
                    ConfirmFooter(
                        order = state.order,
                        onConfirm = state.onApproveClick,
                        modifier = Modifier.dropAway(exit = progress.exit, isDelayed = false),
                    )
                },
            )
        }
    }
}

@Composable
private fun ConfirmOrderTopBar(state: CollectiblesPackOpeningUM, exit: Float) {
    val isNavigationVisible = (state as? CollectiblesPackOpeningUM.Content)?.isNavigationVisible != false

    TangemTopNavigation(
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        modifier = Modifier.graphicsLayer {
            alpha = PackLaunchMotion.contentAlpha(exit, isDelayed = false)
        },
        // The slots keep their size: without a button the bar collapses to the title's height
        startButton = {
            NavigationButtonSlot(isVisible = isNavigationVisible) {
                TangemButton.Back(onClick = state.onBackClick)
            }
        },
        endButton = {
            NavigationButtonSlot(isVisible = isNavigationVisible) {
                TangemButton.Close(onClick = state.onCloseClick)
            }
        },
        contentColumn = {
            TangemNavigationText(
                text = resourceReference(R.string.collectibles_confirm_order_screen_title),
                role = TangemNavigationText.Role.Title,
            )
        },
    )
}

@Composable
private fun NavigationButtonSlot(isVisible: Boolean, button: @Composable () -> Unit) {
    Box(modifier = Modifier.size(44.dp)) {
        AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
            button()
        }
    }
}

@Composable
internal fun ConfirmOrderLayout(
    contentPadding: PaddingValues,
    pack: @Composable (Modifier) -> Unit,
    rows: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val packWidth: Dp = packWidth(maxWidth = maxWidth, maxHeight = maxHeight)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpacerH(height = PACK_MIN_TOP_GAP)
                Spacer(modifier = Modifier.weight(1f))
                pack(Modifier.size(width = packWidth, height = packWidth * PACK_ASPECT))
                SpacerH(height = PACK_MIN_BOTTOM_GAP)
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        rows()
        SpacerH(height = 16.dp)
        footer()
    }
}

internal fun packWidth(maxWidth: Dp, maxHeight: Dp): Dp {
    val heightLimited = (maxHeight - PACK_MIN_TOP_GAP - PACK_MIN_BOTTOM_GAP) / PACK_ASPECT
    return minOf(maxWidth * PACK_WIDTH_FRACTION, heightLimited).coerceAtLeast(0.dp)
}

@Composable
private fun ConfirmOrderError(state: CollectiblesPackOpeningUM.Error, contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CollectiblesStatus(
            message = state.message,
            onReloadClick = state.onReloadClick,
            isReloading = state.isReloading,
        )
    }
}

private fun Modifier.dropAway(exit: Float, isDelayed: Boolean): Modifier = graphicsLayer {
    translationY = PackLaunchMotion.contentShift(exit = exit, isDelayed = isDelayed).toPx()
    alpha = PackLaunchMotion.contentAlpha(exit = exit, isDelayed = isDelayed)
}

@Composable
private fun PackImage(imageUrl: String?, progress: ConfirmProgress, modifier: Modifier = Modifier) {
    var isImageLoading by remember(imageUrl) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .offset(x = PackLaunchMotion.offsetX(progress), y = PackLaunchMotion.offsetY(progress))
            .graphicsLayer {
                val packScale = PackLaunchMotion.scale(progress)
                scaleX = packScale
                scaleY = packScale
                rotationX = PackLaunchMotion.rotationX(progress)
                rotationY = PackLaunchMotion.rotationY(progress)
                rotationZ = PackLaunchMotion.rotationZ(progress)
                cameraDistance = PackLaunchMotion.cameraDistance(packWidthPx = size.width)
                alpha = PackLaunchMotion.alpha(progress)
            },
    ) {
        CollectiblesPackImage(
            imageUrl = imageUrl,
            glowRadius = 48.dp,
            onLoadingChange = { isImageLoading = it },
            modifier = Modifier.matchParentSize(),
            errorContent = { CollectiblesPackImageErrorCard() },
        )

        if (isImageLoading) {
            TangemShimmer(modifier = Modifier.matchParentSize(), radius = PACK_IMAGE_CARD_RADIUS)
        }
    }
}

@Composable
private fun PriceRows(rows: ImmutableList<PriceRowUM>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        rows.forEach { row ->
            TangemRow(
                divider = row.hasDivider,
                titleSlot = {
                    if (row.isEmphasised) {
                        TangemRowText(text = row.labelRef, role = TangemRowTextRole.Title)
                    } else {
                        Text(
                            text = row.labelRef.resolveReference(),
                            style = TangemTheme.typography3.body.medium,
                            color = TangemTheme.colors3.text.secondary,
                        )
                    }
                },
                valueSlot = { TangemRowText(text = row.value, role = TangemRowTextRole.Value) },
            )
        }
    }
}

@Composable
private fun ConfirmFooter(order: OrderUM, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    TangemFooter(
        modifier = modifier,
        fadeEnabled = false,
        blurBackground = false,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            isEnabled = order.button.isEnabled,
            text = order.button.textRef,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}