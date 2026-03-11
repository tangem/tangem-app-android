package com.tangem.feature.swap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.tangem.common.ui.bottomsheet.permission.GiveTxPermissionBottomSheet
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionBottomSheetConfig
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SwapTokenScreenTestTags
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.states.ChooseFeeBottomSheetConfig
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.presentation.R

@Composable
internal fun SwapScreen(stateHolder: SwapStateHolder, feeSelectorBlockComponent: SwapFeeSelectorBlockComponent?) {
    BackHandler(onBack = stateHolder.onBackClicked)

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            AppBarWithBackButton(
                text = stringResourceSafe(R.string.common_swap),
                onBackClick = stateHolder.onBackClicked,
                iconRes = R.drawable.ic_close_24,
            )
        },
        contentWindowInsets = WindowInsetsZero,
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->

        SwapScreenContent(
            state = stateHolder,
            feeBlock = if (feeSelectorBlockComponent != null) {
                @Composable { modifier: Modifier ->
                    feeSelectorBlockComponent.Content(
                        modifier = Modifier
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
                is GiveTxPermissionBottomSheetConfig -> GiveTxPermissionBottomSheet(config = config)
                is ChooseProviderBottomSheetConfig -> ChooseProviderBottomSheet(config = config)
                is ChooseFeeBottomSheetConfig -> ChooseFeeBottomSheet(config = config)
            }
        }
    }
}