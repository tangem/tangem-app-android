package com.tangem.feature.swap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.states.ChooseFeeBottomSheetConfig
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.models.states.GivePermissionBottomSheetConfig

@Composable
internal fun SwapScreen(stateHolder: SwapStateHolder) {
    BackHandler(onBack = stateHolder.onBackClicked)

    Scaffold(
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->

        SwapScreenContent(
            state = stateHolder,
            modifier = Modifier.padding(scaffoldPaddings),
        )

        stateHolder.bottomSheetConfig?.let { config ->
            when (config.content) {
                is GivePermissionBottomSheetConfig -> {
                    SwapPermissionBottomSheet(config = config)
                }
                is ChooseProviderBottomSheetConfig -> {
                    ChooseProviderBottomSheet(config = config)
                }
                is ChooseFeeBottomSheetConfig -> {
                    ChooseFeeBottomSheet(config = config)
                }
            }
        }
    }
}