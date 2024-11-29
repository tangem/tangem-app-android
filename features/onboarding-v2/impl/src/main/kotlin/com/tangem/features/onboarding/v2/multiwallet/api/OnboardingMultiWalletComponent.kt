package com.tangem.features.onboarding.v2.multiwallet.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.navigation.inner.InnerNavigationHolder
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.onboarding.v2.TitleProvider

interface OnboardingMultiWalletComponent : ComposableContentComponent, InnerNavigationHolder {

    data class Params(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
        val withSeedPhraseFlow: Boolean,
        val onDone: (UserWallet) -> Unit,
    )

    interface Factory : ComponentFactory<Params, OnboardingMultiWalletComponent>
}
