package com.tangem.features.polymarket.impl.main.addfunds

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.navigate
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent

internal fun AppComponentContext.polymarketFundingSheetSlot(
    navigation: SlotNavigation<PolymarketFundingSheetConfig>,
    callback: PolymarketAddFundsComponent.ModelCallback,
    tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
): Value<ChildSlot<PolymarketFundingSheetConfig, ComposableBottomSheetComponent>> = childSlot(
    source = navigation,
    serializer = null,
    key = "fundingSheet",
    handleBackButton = false,
    childFactory = { config, componentContext ->
        when (config) {
            PolymarketFundingSheetConfig.AddFunds -> PolymarketAddFundsComponent(
                appComponentContext = childByContext(componentContext),
                params = PolymarketAddFundsComponent.Params(callback = callback),
            )
            is PolymarketFundingSheetConfig.Receive -> tokenReceiveComponentFactory.create(
                context = childByContext(componentContext),
                params = TokenReceiveComponent.Params(
                    config = config.config,
                    onDismiss = { navigation.dismissIfShown(config) },
                ),
            )
        }
    },
)

internal fun SlotNavigation<PolymarketFundingSheetConfig>.dismissIfShown(config: PolymarketFundingSheetConfig) {
    navigate { current -> current.takeUnless { it == config } }
}