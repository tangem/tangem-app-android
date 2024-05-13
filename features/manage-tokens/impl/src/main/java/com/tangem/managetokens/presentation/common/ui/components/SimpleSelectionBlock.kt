package com.tangem.managetokens.presentation.common.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

@Composable
fun SimpleSelectionBlock(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    roundedCorners: Boolean = true,
) {
    Column(
        modifier = modifier
            .then(
                if (roundedCorners) {
                    Modifier.clip(shape = RoundedCornerShape(TangemTheme.dimens.radius16))
                } else {
                    Modifier
                },
            )
            .background(color = TangemTheme.colors.background.action)
            .clickable { onClick() }
            .padding(
                horizontal = TangemTheme.dimens.spacing20,
                vertical = TangemTheme.dimens.spacing16,
            )
            .fillMaxWidth(),
    ) {
        Text(
            text = title,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
        )
        SpacerH(height = TangemTheme.dimens.spacing4)
        Text(
            text = subtitle,
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body2,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_SimpleSelectionBlock() {
    TangemThemePreview {
        SimpleSelectionBlock(title = "Wallet", subtitle = "Family Wallet", onClick = { })
    }
}
