package com.tangem.feature.tester.presentation.featuretoggles.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefresh
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import com.tangem.feature.tester.presentation.common.components.notification.CustomSetupNotification
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
        stickyHeader { TopBarWithRefresh(state = state.topBar) }

        if (state.topBar.refreshButton.isVisible) {
            item(key = "warning_notification", contentType = "warning_notification") {
                CustomSetupNotification(
                    subtitle = resourceReference(
                        id = R.string.feature_toggles_custom_setup_warning_description,
                        wrappedList(state.appVersion),
                    ),
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
            }
        }

        items(
            items = state.featureToggles,
            key = TesterFeatureToggle::name,
            contentType = { "feature_toggle_item" },
        ) { featureToggle ->
            FeatureToggleItem(
                toggle = featureToggle,
                onCheckedChange = { isChange -> state.onToggleValueChange(featureToggle.name, isChange) },
            )
        }

        item {
            PrimaryButton(
                text = stringResourceSafe(id = R.string.restart_app),
                onClick = state.onRestartAppClick,
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
            .padding(
                horizontal = TangemTheme.dimens.spacing18,
                vertical = TangemTheme.dimens.spacing8,
            ),
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

        TangemSwitch(onCheckedChange = onCheckedChange, checked = toggle.isEnabled)
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFeatureTogglesScreen() {
    TangemThemePreview {
        var isCustomSetup by remember { mutableStateOf(value = true) }

        FeatureTogglesScreen(
            state = FeatureTogglesContentState(
                topBar = TopBarWithRefreshUM(
                    titleResId = R.string.feature_toggles,
                    onBackClick = {},
                    refreshButton = TopBarWithRefreshUM.RefreshButton(
                        isVisible = isCustomSetup,
                        onRefreshClick = { isCustomSetup = false },
                    ),
                ),
                appVersion = "5.15",
                featureToggles = persistentListOf(
                    TesterFeatureToggle(name = "FEATURE_TOGGLE_1", isEnabled = true),
                    TesterFeatureToggle(name = "FEATURE_TOGGLE_2", isEnabled = false),
                ),
                onToggleValueChange = { _, _ -> isCustomSetup = true },
                onRestartAppClick = {},
            ),
        )
    }
}