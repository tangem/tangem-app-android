package com.tangem.features.payment.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewColumn

@Composable
public fun PaymentOnboardingFeatureBlock(
    iconPainter: Painter,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(width = 24.dp, height = 24.dp),
            tint = TangemTheme.colors.icon.accent,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = description,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun preview() = TangemThemePreviewColumn {
    PaymentOnboardingFeatureBlock(
        iconPainter = painterResource(com.tangem.core.ui.R.drawable.ic_credit_card_add_24),
        title = "Spend your assets anywhere",
        description = "With digital card that works with Apple Pay and Google Pay",
    )
}