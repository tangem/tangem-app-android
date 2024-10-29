package com.tangem.features.onboarding.v2.wallet12.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.navigation.inner.InnerNavigationHolder
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse

interface OnboardingWallet12Component : ComposableContentComponent, InnerNavigationHolder {

    data class Params(val scanResponse: ScanResponse)

    interface Factory : ComponentFactory<Params, OnboardingWallet12Component>
}
