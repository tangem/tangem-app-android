package com.tangem.feature.tester.presentation.featuretoggles.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.featuretoggles.models.TesterFeatureToggle
import com.tangem.feature.tester.presentation.featuretoggles.state.FeatureTogglesContentState
import kotlinx.collections.immutable.persistentListOf

/**
 * Screen with feature toggles list
 *
 * @param state screen state
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FeatureTogglesScreen(state: FeatureTogglesContentState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary),
    ) {
        stickyHeader {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = stringResource(id = R.string.feature_toggles),
            )
        }
        items(state.featureToggles) { featureToggle ->
            FeatureToggleItem(
                toggle = featureToggle,
                onCheckedChange = { isChange -> state.onToggleValueChange(featureToggle.name, isChange) },
            )
        }
        item {
            PrimaryButton(
                text = stringResource(id = R.string.apply_changes),
                onClick = state.onApplyChangesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TangemTheme.dimens.spacing16),
            )
        }
    }
}

@Composable
private fun FeatureToggleItem(toggle: TesterFeatureToggle, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing18),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = toggle.name,
            modifier = Modifier.weight(1f),
            color = TangemTheme.colors.text.primary1,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography.body2,
        )
        Switch(
            checked = toggle.isEnabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TangemTheme.colors.control.checked,
                uncheckedThumbColor = TangemTheme.colors.control.unchecked,
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFeatureTogglesScreen() {
    TangemThemePreview {
        FeatureTogglesScreen(
            state = FeatureTogglesContentState(
                featureToggles = persistentListOf(
                    TesterFeatureToggle(name = "FEATURE_TOGGLE_1", isEnabled = true),
                    TesterFeatureToggle(name = "FEATURE_TOGGLE_2", isEnabled = false),
                ),
                onToggleValueChange = { _, _ -> },
                onBackClick = {},
                onApplyChangesClick = {},
            ),
        )
    }
}