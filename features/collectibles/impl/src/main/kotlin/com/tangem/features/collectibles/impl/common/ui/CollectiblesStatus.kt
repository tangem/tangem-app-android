package com.tangem.features.collectibles.impl.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_binoculars_20

@Composable
internal fun CollectiblesStatus(message: TextReference, onReloadClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onReloadClick == null) {
            TangemSurface(
                modifier = Modifier.size(40.dp),
                color = TangemTheme.colors3.bg.opaque.secondary,
                shape = CircleShape,
            ) {
                TangemIcon(
                    tangemIconUM = TangemIconUM.Icon(
                        imageVector = Icons.ic_binoculars_20,
                        tintReference = { TangemTheme.colors3.icon.secondary },
                    ),
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            TangemButton(
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X10,
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_arrow_refresh_20),
                onClick = onReloadClick,
            )
        }

        Text(
            text = message.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesStatusPreview() {
    TangemThemePreviewRedesign {
        Column {
            CollectiblesStatus(
                message = stringReference("Failed to load cards list.\nTap to reload"),
                onReloadClick = {},
            )
            CollectiblesStatus(
                message = stringReference("No results. Try to change your wording\nor explore from the list"),
                onReloadClick = null,
            )
        }
    }
}