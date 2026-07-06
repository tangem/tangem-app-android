package com.tangem.feature.swap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SwapTokenScreenTestTags
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.presentation.R

@Composable
internal fun SwapScreen(stateHolder: SwapStateHolder, feeSelectorBlockComponent: SwapFeeSelectorBlockComponent?) {
    BackHandler(onBack = stateHolder.onBackClicked)

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = { SwapTopBar(stateHolder = stateHolder) },
        contentWindowInsets = WindowInsetsZero,
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->

        SwapScreenContent(
            state = stateHolder,
            feeBlock = if (feeSelectorBlockComponent != null) {
                @Composable { modifier: Modifier ->
                    feeSelectorBlockComponent.Content(
                        modifier = modifier
                            .clip(TangemTheme.shapes.roundedCornersXMedium)
                            .background(TangemTheme.colors.background.action),
                    )
                }
            } else {
                null
            },
            modifier = Modifier
                .padding(scaffoldPaddings)
                .testTag(SwapTokenScreenTestTags.CONTAINER),
        )

        if (stateHolder.bottomSheetConfig != null) {
            val config = stateHolder.bottomSheetConfig

            when (config.content) {
                is ChooseProviderBottomSheetConfig -> ChooseProviderBottomSheet(config = config)
            }
        }
    }
}

@Composable
private fun SwapTopBar(stateHolder: SwapStateHolder) {
    var shouldShowModeMenu by rememberSaveable { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        AppBarWithBackButtonAndIcon(
            text = stringResourceSafe(stateHolder.titleId),
            backIconRes = R.drawable.ic_close_24,
            iconRes = R.drawable.ic_more_vertical_24,
            onIconClick = {
                stateHolder.onSwapTypeMenuOpened()
                shouldShowModeMenu = true
            },
            onBackClick = stateHolder.onBackClicked,
        )
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            TangemDropdownMenu(
                expanded = shouldShowModeMenu,
                modifier = Modifier.background(TangemTheme.colors.background.primary),
                offset = DpOffset(x = TangemTheme.dimens.spacing20, y = 44.dp),
                onDismissRequest = { shouldShowModeMenu = false },
                content = {
                    SwapUiModeMenuItem(
                        title = stringResourceSafe(R.string.swap_simple_mode),
                        isSelected = stateHolder.swapUIMode == SwapUIMode.Simple,
                        onClick = {
                            shouldShowModeMenu = false
                            stateHolder.onSwapUIModeChange(SwapUIMode.Simple)
                        },
                    )
                    SwapUiModeMenuItem(
                        title = stringResourceSafe(R.string.swap_detailed_mode),
                        isSelected = stateHolder.swapUIMode == SwapUIMode.Detailed,
                        onClick = {
                            shouldShowModeMenu = false
                            stateHolder.onSwapUIModeChange(SwapUIMode.Detailed)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun SwapUiModeMenuItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography.button,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_24),
                tint = TangemTheme.colors.icon.primary1,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}