package com.tangem.feature.tester.presentation.environments.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.environments.state.EnvironmentTogglesScreenUM
import kotlinx.collections.immutable.persistentSetOf

/**
 * Screen with environment toggles list
 *
 * @param uiModel screen state
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EnvironmentTogglesScreen(uiModel: EnvironmentTogglesScreenUM) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary),
    ) {
        stickyHeader {
            AppBarWithBackButton(
                onBackClick = uiModel.onBackClick,
                text = stringResourceSafe(id = uiModel.title),
            )
        }

        itemsIndexed(
            items = uiModel.apiInfoList.toTypedArray(),
            key = { _, info -> info.name },
        ) { index, info ->
            EnvironmentButtons(
                uiModel = info,
                onSelect = { isChange -> uiModel.onEnvironmentSelect(info.name, isChange) },
                modifier = Modifier.padding(
                    bottom = if (index == uiModel.apiInfoList.toTypedArray().lastIndex) {
                        TangemTheme.dimens.spacing0
                    } else {
                        TangemTheme.dimens.spacing10
                    },
                ),
            )
        }
    }
}

@Composable
private fun EnvironmentButtons(
    uiModel: EnvironmentTogglesScreenUM.ApiInfoUM,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = uiModel.name,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                style = TangemTheme.typography.h3,
            )

            AnimatedContent(targetState = uiModel.url, label = "") {
                Text(
                    text = it,
                    color = TangemTheme.colors.text.accent,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TangemTheme.typography.body2,
                )
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            uiModel.environments.onEachIndexed { index, environment ->
                key(environment) {
                    SegmentedButton(
                        selected = environment == uiModel.select,
                        onClick = { onSelect(environment) },
                        shape = when (index) {
                            0 -> RoundedCornerShape(
                                topStart = TangemTheme.dimens.radius12,
                                bottomStart = TangemTheme.dimens.radius12,
                            )
                            uiModel.environments.toTypedArray().lastIndex -> RoundedCornerShape(
                                topEnd = TangemTheme.dimens.radius12,
                                bottomEnd = TangemTheme.dimens.radius12,
                            )
                            else -> RectangleShape
                        },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = TangemTheme.colors.control.checked,
                            activeContentColor = TangemTheme.colors.text.primary2,
                            inactiveContainerColor = TangemTheme.colors.control.unchecked,
                            inactiveContentColor = TangemTheme.colors.text.primary1,
                        ),
                        border = BorderStroke(0.dp, TangemTheme.colors.background.tertiary),
                    ) {
                        Text(text = environment, style = TangemTheme.typography.subtitle2)
                    }
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFeatureTogglesScreen() {
    TangemThemePreview {
        var select by remember { mutableStateOf(ApiEnvironment.DEV.name) }

        EnvironmentTogglesScreen(
            uiModel = EnvironmentTogglesScreenUM(
                title = R.string.environment_toggles,
                apiInfoList = persistentSetOf(
                    EnvironmentTogglesScreenUM.ApiInfoUM(
                        name = ApiConfig.ID.Express.name,
                        select = select,
                        url = "https://api.express.tangem.com",
                        environments = persistentSetOf(
                            ApiEnvironment.DEV.name,
                            ApiEnvironment.STAGE.name,
                            ApiEnvironment.PROD.name,
                        ),
                    ),
                    EnvironmentTogglesScreenUM.ApiInfoUM(
                        name = ApiConfig.ID.TangemTech.name,
                        select = select,
                        url = "https://api.express.tangem.com",
                        environments = persistentSetOf(
                            ApiEnvironment.DEV.name,
                            ApiEnvironment.PROD.name,
                        ),
                    ),
                ),
                onEnvironmentSelect = { _: String, s1: String -> select = s1 },
                onBackClick = {},
            ),
        )
    }
}