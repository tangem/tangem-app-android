package com.tangem.features.hotwallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface HotAccessCodeRequestComponent : ComposableContentComponent, HotWalletPasswordRequester {

    interface Factory : ComponentFactory<Unit, HotAccessCodeRequestComponent>
}