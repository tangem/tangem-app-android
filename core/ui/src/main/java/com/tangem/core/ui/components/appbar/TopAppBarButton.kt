package com.tangem.core.ui.components.appbar

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.res.TangemTheme

@Composable
fun TopAppBarButton(button: TopAppBarButtonUM, tint: Color, modifier: Modifier = Modifier) {
    IconButton(
        modifier = modifier.size(TangemTheme.dimens.size32),
        onClick = button.onIconClicked,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(id = button.iconRes),
            tint = tint,
            contentDescription = null,
        )
    }
}
