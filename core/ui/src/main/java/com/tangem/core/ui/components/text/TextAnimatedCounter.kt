package com.tangem.core.ui.components.text

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.tangem.core.ui.res.TangemTheme

@Composable
fun TextAnimatedCounter(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TangemTheme.typography.caption1,
) {
    var oldText by remember {
        mutableStateOf(text)
    }
    SideEffect {
        oldText = text
    }
    Row(modifier = modifier) {
        for (i in text.indices) {
            val oldChar = oldText.getOrNull(i)
            val newChar = text[i]
            val targetChar = if (oldChar == newChar) {
                oldText[i]
            } else {
                text[i]
            }
            AnimatedContent(
                targetState = targetChar,
                transitionSpec = {
                    slideInVertically { it }.togetherWith(slideOutVertically { -it })
                },
            ) { char ->
                Text(
                    text = char.toString(),
                    style = style,
                    softWrap = false,
                )
            }
        }
    }
}

/** Animates digits by decimal place; affixes stay static and missing places fade to or from empty space. */
@Composable
fun TextAnimatedCounter(
    text: String,
    decimalSeparator: Char,
    modifier: Modifier = Modifier,
    style: TextStyle = TangemTheme.typography.caption1,
    animationSpec: TweenSpec<IntOffset> = tween(),
) {
    val number = remember(text, decimalSeparator) { CounterNumber.parse(text, decimalSeparator) }
    if (number == null) {
        Text(text = text, modifier = modifier, style = style, softWrap = false, maxLines = 1)
        return
    }
    val initialNumber = remember { number }
    // Keep empty places composed: another price can arrive while their previous digit is still exiting.
    var retainedIntegerPlaces by remember { mutableIntStateOf(number.integer.length) }
    var retainedFractionPlaces by remember { mutableIntStateOf(number.fraction.length) }
    val integerPlaces = maxOf(retainedIntegerPlaces, number.integer.length)
    val fractionPlaces = maxOf(retainedFractionPlaces, number.fraction.length)
    SideEffect {
        retainedIntegerPlaces = integerPlaces
        retainedFractionPlaces = fractionPlaces
    }
    Row(
        modifier = modifier.clearAndSetSemantics { this.text = AnnotatedString(text) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = number.prefix, style = style, softWrap = false)
        for (place in integerPlaces - 1 downTo 0) {
            key(place) {
                CounterDigit(
                    character = number.integerDigit(place),
                    initialCharacter = initialNumber.integerDigit(place),
                    style = style,
                    animationSpec = animationSpec,
                )
            }
        }
        CounterDigit(
            character = decimalSeparator.takeIf { number.fraction.isNotEmpty() },
            initialCharacter = decimalSeparator.takeIf { initialNumber.fraction.isNotEmpty() },
            style = style,
            animationSpec = animationSpec,
        )
        for (place in 0 until fractionPlaces) {
            key(place) {
                CounterDigit(
                    character = number.fraction.getOrNull(place),
                    initialCharacter = initialNumber.fraction.getOrNull(place),
                    style = style,
                    animationSpec = animationSpec,
                )
            }
        }
        Text(text = number.suffix, style = style, softWrap = false)
    }
}

@Composable
private fun CounterDigit(
    character: Char?,
    initialCharacter: Char?,
    style: TextStyle,
    animationSpec: TweenSpec<IntOffset>,
) {
    val state = remember { MutableTransitionState(initialCharacter) }
    state.targetState = character
    val transition = rememberTransition(state, label = "Digit")
    transition.AnimatedContent(
        modifier = Modifier.clipToBounds(),
        transitionSpec = {
            val from = initialState
            val to = targetState
            val fadeSpec = tween<Float>(
                durationMillis = animationSpec.durationMillis,
                delayMillis = animationSpec.delay,
                easing = animationSpec.easing,
            )
            val enter = if (from?.isDigit() == true && to?.isDigit() == true) {
                slideInVertically(animationSpec) { it }
            } else {
                fadeIn(fadeSpec)
            }
            val exit = if (from?.isDigit() == true && to?.isDigit() == true) {
                slideOutVertically(animationSpec) { -it }
            } else {
                fadeOut(fadeSpec)
            }
            (enter togetherWith exit).using(
                SizeTransform { _, _ ->
                    tween(
                        durationMillis = animationSpec.durationMillis,
                        delayMillis = animationSpec.delay,
                        easing = animationSpec.easing,
                    )
                },
            )
        },
    ) { value ->
        Text(text = value?.toString().orEmpty(), style = style, softWrap = false, maxLines = 1)
    }
}