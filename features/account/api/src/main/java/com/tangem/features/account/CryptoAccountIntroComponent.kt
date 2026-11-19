package com.tangem.features.account

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/** Intro to crypto accounts that settles the wallet after Continue, then opens account creation in it. */
interface CryptoAccountIntroComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Unit, CryptoAccountIntroComponent>
}