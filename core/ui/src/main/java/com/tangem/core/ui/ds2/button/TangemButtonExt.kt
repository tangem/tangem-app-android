package com.tangem.core.ui.ds2.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.core.ui.res.generated.icons.ic_cross_20

@Composable
@NonRestartableComposable
fun TangemButton.Back(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Material,
        size = TangemButton.Size.X11,
        iconStart = TangemIconUM.Icon(Icons.ic_chevron_left_20),
        onClick = onClick,
    )
}

@Composable
@NonRestartableComposable
fun TangemButton.Close(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Material,
        size = TangemButton.Size.X11,
        iconStart = TangemIconUM.Icon(Icons.ic_cross_20),
        onClick = onClick,
    )
}

@Composable
@NonRestartableComposable
fun TangemButton.GroupEntry(iconUM: TangemIconUM, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Ghost,
        size = TangemButton.Size.X9,
        iconStart = iconUM,
        onClick = onClick,
    )
}

@Composable
@NonRestartableComposable
fun TangemButton.GroupEntry(imageVector: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GroupEntry(iconUM = TangemIconUM.Icon(imageVector), modifier = modifier, onClick = onClick)
}