package com.tangem.features.collectibles.impl.confirmorder.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.common.ui.CollectiblesPackImage
import com.tangem.features.collectibles.impl.common.ui.CollectiblesPackImageErrorCard
import com.tangem.features.collectibles.impl.common.ui.CollectiblesStatus
import com.tangem.features.collectibles.impl.common.ui.PACK_IMAGE_CARD_RADIUS
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM.OrderUM
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM.PriceRowUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference

private val ROWS_MARGIN = 8.dp

private val PACK_GLOW_RADIUS = 48.dp

@Composable
internal fun CollectiblesConfirmOrderScreen(state: CollectiblesConfirmOrderUM, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    // Kept out of the model: a StateFlow would recompose the screen on every animation frame
    val dip = remember { Animatable(0f) }
    val launchUp = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val progress = ConfirmProgress(dip = dip.value, launch = launchUp.value, exit = exit.value)

    var footerHeight by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TangemTopNavigation(
                title = resourceReference(R.string.collectibles_confirm_order_screen_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBackClick,
                // Fades in place: dropping it would slide it through the pack
                modifier = Modifier.graphicsLayer {
                    alpha = PackLaunch.contentAlpha(progress.exit, isDelayed = false)
                },
            )
        },
        overlay = {
            when (state) {
                is CollectiblesConfirmOrderUM.Loading -> CollectiblesConfirmOrderFooterSkeleton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size -> footerHeight = size.height },
                )
                is CollectiblesConfirmOrderUM.Error -> Unit
                is CollectiblesConfirmOrderUM.Content -> ConfirmFooter(
                    order = state.order,
                    onConfirm = {
                        state.onApproveClick()
                        scope.launch {
                            // The content exit runs alongside the pack's beats, not after them
                            coroutineScope {
                                launch {
                                    exit.animateTo(1f, tween(ConfirmTiming.EXIT_MS, easing = FastOutLinearInEasing))
                                }
                                dip.animateTo(1f, tween(ConfirmTiming.DIP_MS, easing = FastOutLinearInEasing))
                                launchUp.animateTo(1f, tween(ConfirmTiming.LAUNCH_MS, easing = LinearOutSlowInEasing))
                            }
                        }
                    },
                    modifier = Modifier
                        .dropAway(exit = progress.exit, isDelayed = false)
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size -> footerHeight = size.height },
                )
            }
        },
    ) { contentPadding ->
        when (state) {
            is CollectiblesConfirmOrderUM.Loading -> CollectiblesConfirmOrderSkeleton(footerHeight = footerHeight)
            is CollectiblesConfirmOrderUM.Error -> ConfirmOrderError(state = state, contentPadding = contentPadding)
            is CollectiblesConfirmOrderUM.Content -> ConfirmOrderContent(
                order = state.order,
                progress = progress,
                footerHeight = footerHeight,
            )
        }
    }
}

@Composable
private fun ConfirmOrderError(state: CollectiblesConfirmOrderUM.Error, contentPadding: PaddingValues) {
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

@Composable
private fun ConfirmOrderContent(order: OrderUM, progress: ConfirmProgress, footerHeight: Int) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val packWidth = with(density) { (constraints.maxWidth * PACK_WIDTH_FRACTION).toDp() }

        PackImage(
            imageUrl = order.packImageUrl,
            progress = progress,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -PACK_LIFT)
                .size(width = packWidth, height = packWidth * PACK_ASPECT),
        )

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            // Delayed: the tapped button leaves first, the rows follow
            PriceRows(
                rows = order.rows,
                modifier = Modifier.dropAway(exit = progress.exit, isDelayed = true),
            )
            Spacer(modifier = Modifier.height(with(density) { footerHeight.toDp() }))
        }
    }
}

private fun Modifier.dropAway(exit: Float, isDelayed: Boolean): Modifier = graphicsLayer {
    translationY = PackLaunch.contentShift(exit = exit, isDelayed = isDelayed).toPx()
    alpha = PackLaunch.contentAlpha(exit = exit, isDelayed = isDelayed)
}

@Composable
private fun PackImage(imageUrl: String?, progress: ConfirmProgress, modifier: Modifier = Modifier) {
    var isImageLoading by remember(imageUrl) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .offset(x = PackLaunch.offsetX(progress), y = PackLaunch.offsetY(progress))
            .graphicsLayer {
                val packScale = PackLaunch.scale(progress)
                scaleX = packScale
                scaleY = packScale
                rotationZ = PackLaunch.rotation(progress)
                alpha = PackLaunch.alpha(progress)
            },
    ) {
        CollectiblesPackImage(
            imageUrl = imageUrl,
            glowRadius = PACK_GLOW_RADIUS,
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
    Column(modifier = modifier.padding(horizontal = ROWS_MARGIN)) {
        rows.forEach { row ->
            TangemRow(
                divider = row.hasDivider,
                titleSlot = {
                    // The DS makes every title primary; the design dims all but the total
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
    TangemFooter(modifier = modifier) {
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