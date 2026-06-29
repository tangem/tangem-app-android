package com.tangem.features.virtualaccount.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24

@Composable
fun TangemCircleActionButton(
    title: TextReference,
    icon: TangemIconUM,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Material,
            size = TangemButton.Size.X14,
            onClick = onClick,
            iconStart = icon,
            isLoading = isLoading,
            isEnabled = isEnabled,
        )
        Text(
            text = title.resolveAnnotatedReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
            autoSize = TextAutoSize.StepBased(
                minFontSize = TangemTheme.typography3.caption.medium.fontSize,
                maxFontSize = TangemTheme.typography3.subheading.medium.fontSize,
            ),
            maxLines = 1,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemCircleActionButtonPreview() {
    TangemThemePreviewRedesign {
        TangemCircleActionButton(
            title = stringReference("Action"),
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_arrow_down_24,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
            onClick = {},
        )
    }
}