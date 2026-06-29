package com.tangem.features.virtualaccount.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_binoculars_20

@Composable
fun TangemEmptyState(
    icon: ImageVector,
    text: TextReference,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens2.x10)
                .background(
                    color = TangemTheme.colors3.bg.opaque.primary,
                    shape = CircleShape,
                )
                .padding(10.dp)
                .then(iconModifier),
            imageVector = icon,
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )

        Text(
            modifier = textModifier,
            textAlign = TextAlign.Center,
            text = text.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemEmptyStatePreview() {
    TangemThemePreviewRedesign {
        TangemEmptyState(
            icon = Icons.ic_binoculars_20,
            text = stringReference("No transactions yet\nStart spending and see history here"),
        )
    }
}