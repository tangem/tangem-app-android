package com.tangem.core.ui.ds2.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_left_20
import com.tangem.core.ui.res.generated.icons.ic_cross_20

@Composable
@NonRestartableComposable
fun TangemButton.Back(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Material,
        iconStart = TangemIconUM.Icon(Icons.ic_arrow_left_20),
        onClick = onClick,
    )
}

@Composable
@NonRestartableComposable
fun TangemButton.Close(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Material,
        iconStart = TangemIconUM.Icon(Icons.ic_cross_20),
        onClick = onClick,
    )
}