package com.tangem.features.yieldlending.impl.promo.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yieldlending.impl.R

@Composable
internal fun YieldLendingPromoContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxWidth()
            .imePadding()
            .systemBarsPadding(),
    ) {
        AppBarWithBackButton(
            onBackClick = {},
            iconRes = R.drawable.ic_back_24,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_analytics_up_24),
                tint = TangemTheme.colors.icon.accent,
                contentDescription = null,
                modifier = Modifier
                    .background(TangemTheme.colors.icon.accent.copy(0.1f), CircleShape)
                    .padding(12.dp)
                    .size(32.dp),
            )
            SpacerH(20.dp)
            Text(
                text = "Maximize your savings",
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH8()
            Label(
                state = LabelUM(
                    text = stringReference("Aave • 5.1% APY"),
                    style = LabelStyle.REGULAR,
                    icon = R.drawable.ic_information_24,
                )
            )
            SpacerH32()
            PromoItem(
                icon = R.drawable.ic_flash_new_24,
                title = stringReference("Cash out instantly"),
                subtitle = stringReference("Send, swap, or sell your funds instantly, anytime you want."),
            )
            SpacerH24()
            PromoItem(
                icon = R.drawable.ic_security_check_24,
                title = stringReference("Decentralized and self-custodial"),
                subtitle = stringReference("Trusted by millions worldwide. Total lended value is \$10.4B."),
            )
            SpacerH24()

        }
        SpacerHMax()
        SecondaryButton(
            text = "How it work?",
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        SpacerH12()
        PrimaryButton(
            text = "Start earning",
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        SpacerH8()
    }
}

@Composable
private fun PromoItem(
    @DrawableRes icon: Int,
    title: TextReference,
    subtitle: TextReference,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldLendingPromoContent_Preview() {
    TangemThemePreview {
        YieldLendingPromoContent(
            onClick = {},
        )
    }
}

// endregion