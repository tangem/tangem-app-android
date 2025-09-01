package com.tangem.core.ui.extensions

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.MultipleClickPreventer

/**
 * Clickable modifier with debounce multiple click in a short period of time
 */
fun Modifier.clickableSingle(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
) = composed {
    val multipleEventsCutter = remember { MultipleClickPreventer.get() }
    Modifier.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = { multipleEventsCutter.processEvent { onClick() } },
        role = role,
        indication = LocalIndication.current,
        interactionSource = remember { MutableInteractionSource() },
    )
}

/**
 * Conditionally applies a modifier based on a boolean condition.
 */
fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

/**
 * Conditionally applies a modifier based on a boolean condition.
 */
@Composable
fun Modifier.conditionalCompose(
    condition: Boolean,
    modifier: @Composable Modifier.() -> Modifier = { Modifier },
    otherModifier: @Composable Modifier.() -> Modifier = { this },
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        otherModifier()
    }
}

/**
 * Border with accent outline when selected
 */
@Composable
fun Modifier.selectedBorder(
    isSelected: Boolean,
    width: Dp = 2.5.dp,
    color: Color = TangemTheme.colors.text.accent,
    radius: Dp = 16.dp,
) = conditionalCompose(
    condition = isSelected,
    modifier = {
        outsetBorder(
            width = width,
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(radius + 2.dp),
        )
            .border(
                width = 1.dp,
                color = color,
                shape = RoundedCornerShape(radius),
            )
            .clip(RoundedCornerShape(radius))
    },
    otherModifier = {
        clip(RoundedCornerShape(radius))
    },
)