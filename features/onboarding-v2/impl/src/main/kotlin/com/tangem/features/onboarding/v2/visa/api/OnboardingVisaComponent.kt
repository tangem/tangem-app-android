package com.tangem.features.onboarding.v2.visa.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.navigation.inner.InnerNavigationHolder
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.TitleProvider

interface OnboardingVisaComponent : ComposableContentComponent, InnerNavigationHolder {

    data class Params(val titleProvider: TitleProvider)

    interface Factory : ComponentFactory<Params, OnboardingVisaComponent>
}