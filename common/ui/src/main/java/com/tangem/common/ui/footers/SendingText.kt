package com.tangem.common.ui.footers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Sending info text with display animation.
 * Must show text only when keyboard is closed
 *
 * @param footerText text to display
 * @param modifier composable modifier
 */
@Composable
fun SendingText(footerText: TextReference, modifier: Modifier = Modifier) {
    var isVisibleProxy by remember { mutableStateOf(footerText != TextReference.EMPTY) }
    val keyboard by keyboardAsState()

    // the text should appear when the keyboard is closed
    LaunchedEffect(footerText != TextReference.EMPTY, keyboard) {
        if (footerText != TextReference.EMPTY && keyboard is Keyboard.Opened) {
            return@LaunchedEffect
        }
        isVisibleProxy = footerText != TextReference.EMPTY
    }

    AnimatedVisibility(
        visible = isVisibleProxy,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = fadeOut(tween(durationMillis = 300)),
        label = "Animate show sending state text",
    ) {
        Text(
            text = footerText.resolveAnnotatedReference(),
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}