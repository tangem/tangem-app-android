package com.tangem.common.ui.expressStatus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
fun ColumnScope.ExpressHideButton(isTerminal: Boolean, isAutoDisposable: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = isTerminal && !isAutoDisposable,
        label = "Hide button visibility animation",
    ) {
        Text(
            text = stringResourceSafe(R.string.express_status_hide_button_text),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.button,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onClick,
                )
                .padding(vertical = 10.dp),
        )
    }
}