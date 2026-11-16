package com.tangem.features.tokenreceive.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_16
import com.tangem.features.tokenreceive.entity.ReceiveExtrasHint
import com.tangem.features.tokenreceive.impl.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ADDRESS_EDGE_LENGTH = 5
private const val PLATE_HIDDEN_SCALE = 0.96f

private const val REVEAL_DELAY_MILLIS = 350L
private const val REVEAL_DURATION_MILLIS = 500
private val RevealEasing = CubicBezierEasing(a = 0.8f, b = 0f, c = 0.2f, d = 1f)

private const val SQUEEZE_SCALE = 0.97f
private const val SQUEEZE_MIN_HOLD_MILLIS = 120L
private val SqueezeIn = spring<Float>(dampingRatio = 0.7f, stiffness = 815f)
private val SqueezeOut = spring<Float>(dampingRatio = 0.48f, stiffness = 273f)

@Suppress("LongParameterList")
@Composable
internal fun ReceiveAddressCard(
    address: String,
    iconState: CurrencyIconState,
    extrasHint: ReceiveExtrasHint,
    ensName: String?,
    networkNotice: TextReference,
    onCopyAddressClick: () -> Unit,
    onCopyEnsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(REVEAL_DELAY_MILLIS)
        reveal.animateTo(targetValue = 1f, animationSpec = tween(REVEAL_DURATION_MILLIS, easing = RevealEasing))
    }
    val squeeze = rememberSqueezeState()

    CardWithNotice(
        reveal = { reveal.value },
        modifier = modifier.graphicsLayer {
            scaleX = squeeze.scale
            scaleY = squeeze.scale
        },
        plate = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomEnd = 28.dp, bottomStart = 28.dp))
                    .background(TangemTheme.colors3.bg.status.infoSubtle),
            ) {
                TangemGlowRing(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = reveal.value },
                    variant = TangemGlowRing.Variant.Info,
                    cornerRadius = 28.dp,
                )
            }
        },
        notice = {
            Text(
                text = networkNotice.resolveReference(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.info,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        card = {
            CardFace(
                address = address,
                iconState = iconState,
                extrasHint = extrasHint,
                ensName = ensName,
                squeeze = squeeze,
                onCopyAddressClick = onCopyAddressClick,
                onCopyEnsClick = onCopyEnsClick,
            )
        },
    )
}

@Suppress("LongParameterList")
@Composable
private fun CardFace(
    address: String,
    iconState: CurrencyIconState,
    extrasHint: ReceiveExtrasHint,
    ensName: String?,
    squeeze: SqueezeState,
    onCopyAddressClick: () -> Unit,
    onCopyEnsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(274.dp)
            .background(TangemTheme.colors3.icon.staticDark, RoundedCornerShape(32.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .squeezeClick(squeeze, onCopyAddressClick)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (ensName != null) 8.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                ReceiveQrCode(
                    content = address,
                    color = TangemTheme.colors3.icon.staticLight,
                    modifier = Modifier.matchParentSize(),
                )
                CurrencyIcon(
                    state = iconState,
                    iconSize = 56.dp,
                    networkBadgeSize = 20.dp,
                    networkBadgeBackground = TangemTheme.colors3.icon.staticDark,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = styledAddress(address),
                    modifier = Modifier.fillMaxWidth(),
                    style = TangemTheme.typography3.subheading.medium,
                    textAlign = TextAlign.Center,
                )
                ExtrasHintBadge(extrasHint)
            }
        }
        if (ensName != null) {
            LightPalette {
                TangemBadge(
                    text = stringReference(ensName),
                    modifier = Modifier.padding(bottom = 16.dp),
                    size = TangemBadge.Size.X6,
                    variant = TangemBadge.Variant.Solid,
                    iconEnd = TangemIconUM.Icon(Icons.ic_copy_16),
                    onClick = onCopyEnsClick,
                )
            }
        }
    }
}

@Composable
private fun ExtrasHintBadge(extrasHint: ReceiveExtrasHint) {
    val text = when (extrasHint) {
        ReceiveExtrasHint.None -> return
        ReceiveExtrasHint.NoMemo -> R.string.receive_bottom_sheet_no_memo_required_message
        ReceiveExtrasHint.NoTag -> R.string.receive_no_tag_required
    }
    LightPalette {
        TangemBadge(
            text = resourceReference(text),
            size = TangemBadge.Size.X6,
            status = TangemBadge.Status.Warning,
        )
    }
}

@Composable
private fun LightPalette(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIsInDarkTheme provides false) {
        TangemThemeRedesign(content = content)
    }
}

@Composable
private fun styledAddress(address: String): AnnotatedString {
    val primary = TangemTheme.colors3.text.staticLight.primary
    val secondary = TangemTheme.colors3.text.staticLight.secondary
    return remember(address, primary, secondary) {
        if (address.length <= ADDRESS_EDGE_LENGTH * 2) {
            AnnotatedString(text = address, spanStyle = SpanStyle(color = primary))
        } else {
            buildAnnotatedString {
                withStyle(SpanStyle(color = primary)) { append(address.take(ADDRESS_EDGE_LENGTH)) }
                withStyle(SpanStyle(color = secondary)) {
                    append(address.substring(ADDRESS_EDGE_LENGTH, address.length - ADDRESS_EDGE_LENGTH))
                }
                withStyle(SpanStyle(color = primary)) { append(address.takeLast(ADDRESS_EDGE_LENGTH)) }
            }
        }
    }
}

@Composable
private fun CardWithNotice(
    reveal: () -> Float,
    modifier: Modifier = Modifier,
    plate: @Composable () -> Unit,
    notice: @Composable () -> Unit,
    card: @Composable () -> Unit,
) {
    Layout(contents = listOf(plate, notice, card), modifier = modifier) { measurables, constraints ->
        val (plateMeasurable, noticeMeasurable, cardMeasurable) = measurables.map { it.single() }
        val cardPlaceable = cardMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val noticePlaceable = noticeMeasurable.measure(Constraints.fixedWidth(cardPlaceable.width))
        val platePlaceable = plateMeasurable.measure(
            Constraints.fixed(width = cardPlaceable.width, height = cardPlaceable.height),
        )
        val noticeHeight = noticePlaceable.height.toFloat()

        layout(width = cardPlaceable.width, height = cardPlaceable.height + noticePlaceable.height) {
            val hiddenOffset = { (1f - reveal()) * noticeHeight / 2 }
            val plateOffset = { hiddenOffset() + reveal() * noticeHeight }
            platePlaceable.placeRelativeWithLayer(x = 0, y = 0) {
                translationY = plateOffset()
                val scale = PLATE_HIDDEN_SCALE + (1f - PLATE_HIDDEN_SCALE) * reveal()
                scaleX = scale
                scaleY = scale
            }
            noticePlaceable.placeRelativeWithLayer(x = 0, y = cardPlaceable.height - noticePlaceable.height) {
                translationY = plateOffset()
                alpha = reveal().coerceIn(0f, 1f)
            }
            cardPlaceable.placeRelativeWithLayer(x = 0, y = 0) {
                translationY = hiddenOffset()
            }
        }
    }
}

@Stable
private class SqueezeState(private val animatable: Animatable<Float, *>) {
    val scale: Float get() = animatable.value

    suspend fun press() = animatable.animateTo(SQUEEZE_SCALE, SqueezeIn)

    suspend fun release() = animatable.animateTo(1f, SqueezeOut)
}

@Composable
private fun rememberSqueezeState(): SqueezeState = remember { SqueezeState(Animatable(1f)) }

@Composable
private fun Modifier.squeezeClick(state: SqueezeState, onClick: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    return this
        .pointerInput(state) {
            detectTapGestures(
                onPress = {
                    val pressedAt = SystemClock.uptimeMillis()
                    val squeeze = scope.launch { state.press() }
                    val isReleased = tryAwaitRelease()
                    scope.launch {
                        if (isReleased) {
                            val remainingHold = SQUEEZE_MIN_HOLD_MILLIS - (SystemClock.uptimeMillis() - pressedAt)
                            if (remainingHold > 0) delay(remainingHold)
                        }
                        squeeze.cancel()
                        state.release()
                    }
                },
                onTap = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnClick()
                },
            )
        }
        .semantics {
            role = Role.Button
            onClick {
                currentOnClick()
                true
            }
        }
}