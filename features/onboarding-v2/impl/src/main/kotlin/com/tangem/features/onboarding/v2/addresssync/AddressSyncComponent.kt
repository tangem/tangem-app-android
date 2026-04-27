package com.tangem.features.onboarding.v2.addresssync

import com.tangem.core.ui.decompose.ComposableContentComponent

interface AddressSyncComponent : ComposableContentComponent {
    data class Params(val isWalletStarted: Boolean)
}