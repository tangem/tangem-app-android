package com.tangem.core.ui.components.text

import androidx.compose.animation.AnimatedContent
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
            val char = if (oldChar == newChar) {
                oldText[i]
            } else {
                text[i]
            }
            AnimatedContent(
                targetState = char,
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