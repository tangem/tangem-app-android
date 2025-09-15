package com.tangem.features.hotwallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester

interface HotAccessCodeRequestComponent : ComposableContentComponent, HotWalletPasswordRequester {

    interface Factory : ComponentFactory<Unit, HotAccessCodeRequestComponent>
}