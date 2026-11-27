package com.tangem.features.account.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.account.CryptoAccountIntroComponent
import com.tangem.features.account.intro.model.CryptoAccountIntroModel
import com.tangem.features.account.intro.ui.CryptoAccountIntroScreen
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.builtins.serializer

internal class DefaultCryptoAccountIntroComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted @Suppress("UnusedPrivateMember") params: Unit,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : CryptoAccountIntroComponent, AppComponentContext by appComponentContext {

    private val model: CryptoAccountIntroModel = getOrCreateModel()

    private val walletSelectorSlot = childSlot(
        source = model.walletPicker.navigation,
        serializer = Unit.serializer(),
        key = "cryptoAccountIntroWalletSelectorSlot",
        handleBackButton = false,
        childFactory = { _, componentContext ->
            portfolioSelectorComponentFactory.create(
                context = childByContext(componentContext),
                params = model.walletPicker.selectorParams,
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val walletSelector by walletSelectorSlot.subscribeAsState()

        CryptoAccountIntroScreen(state = state, modifier = modifier)

        walletSelector.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : CryptoAccountIntroComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCryptoAccountIntroComponent
    }
}