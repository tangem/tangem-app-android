package com.tangem.features.onramp.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImage
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.settings.entity.OnrampSettingsItemUM
import com.tangem.features.onramp.settings.entity.OnrampSettingsUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun OnrampSettingsContent(state: OnrampSettingsUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsetsZero,
        containerColor = TangemTheme.colors.background.secondary,
        topBar = {
            AppBarWithBackButton(
                modifier = Modifier.statusBarsPadding(),
                onBackClick = state.onBack,
                text = stringResource(id = R.string.onramp_settings_title),
                iconRes = R.drawable.ic_close_24,
            )
        },
        content = { innerPadding ->
            Content(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = TangemTheme.dimens.spacing16),
                items = state.items,
            )
        },
    )
}

@Composable
private fun Content(items: ImmutableList<OnrampSettingsItemUM>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        items.fastForEach { model ->
            key(model) {
                when (model) {
                    is OnrampSettingsItemUM.Residence -> ResidenceSection(model)
                }
            }
        }
    }
}

@Composable
private fun ResidenceSection(state: OnrampSettingsItemUM.Residence, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                .background(TangemTheme.colors.background.primary)
                .clickable(onClick = state.onClick)
                .padding(TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.onramp_settings_residence),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
            )
            SpacerWMax()
            AsyncImage(
                modifier = Modifier.size(TangemTheme.dimens.size20),
                model = state.flagUrl,
                contentDescription = null,
            )
            Text(
                text = state.countryName,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body2,
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
        Text(
            text = stringResource(id = R.string.onramp_settings_residence_description),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
        )
    }
}