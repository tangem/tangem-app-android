package com.tangem.features.yieldlending.impl.main.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yieldlending.impl.R
import com.tangem.features.yieldlending.impl.main.entity.YieldLendingUM
import com.tangem.utils.StringsSigns

@Composable
internal fun YieldLendingBlockContent(
    yieldLendingUM: YieldLendingUM, onClick: () -> Unit,
    modifier: Modifier =
        Modifier,
) {
    AnimatedContent(
        targetState = yieldLendingUM,
        modifier = modifier,
    ) { lendingUM ->
        when (lendingUM) {
            is YieldLendingUM.Initial -> LendingInitial(onClick = onClick)
            YieldLendingUM.Loading -> LendingLoading()
            YieldLendingUM.Content -> LendingContent(onClick = onClick)
        }
    }
}

@Composable
private fun LendingInitial(onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_analytics_up_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
                modifier = Modifier
                    .background(TangemTheme.colors.icon.accent.copy(alpha = 0.1f), CircleShape)
                    .padding(6.dp)
                    .size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Earn 5.1% per year", // todo yield lending localize
                    style = TangemTheme.typography.button,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = "Make your money work — earn interest on your balance.", // todo yield lending localize
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
        SecondaryButton(
            text = "Learn more", // todo yield lending localize
            onClick = onClick,
            size = TangemButtonSize.WideAction,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LendingContent(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Earning on your balance", // todo yield lending localize
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "\$0.000049", // todo yield lending from data
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = StringsSigns.DOT,
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.tertiary,
                )
                Text(
                    text = "5.1% APY", // todo yield lending from data
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun LendingLoading(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .padding(12.dp)
    ) {
        Text(
            text = "Earning on your balance", // todo yield lending localize
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        TextShimmer(
            style = TangemTheme.typography.subtitle2,
            text = "Earning on your balance ",
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldLendingBlockContent_Preview(
    @PreviewParameter(PreviewProvider::class) params: YieldLendingUM,
) {
    TangemThemePreview {
        YieldLendingBlockContent(params, {})
    }
}

private class PreviewProvider : PreviewParameterProvider<YieldLendingUM> {
    override val values: Sequence<YieldLendingUM>
        get() = sequenceOf(
            YieldLendingUM.Initial,
            YieldLendingUM.Content,
            YieldLendingUM.Loading,
        )
}
// endregion