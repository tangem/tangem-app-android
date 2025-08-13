package com.tangem.core.ui.components.appbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
fun TopAppBarButton(button: TopAppBarButtonUM, tint: Color, modifier: Modifier = Modifier) {
    when (button) {
        is TopAppBarButtonUM.Icon -> {
            IconButton(
                enabled = button.enabled,
                modifier = modifier.size(TangemTheme.dimens.size32),
                onClick = button.onClicked,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    painter = painterResource(id = button.iconRes),
                    tint = tint,
                    contentDescription = null,
                )
            }
        }
        is TopAppBarButtonUM.Text -> {
            Text(
                modifier = modifier
                    .conditional(button.enabled) {
                        clickable { button.onClicked() }
                    }
                    .padding(4.dp),
                text = button.text.resolveReference(),
                color = tint,
                style = TangemTheme.typography.body1,
            )
        }
    }
}