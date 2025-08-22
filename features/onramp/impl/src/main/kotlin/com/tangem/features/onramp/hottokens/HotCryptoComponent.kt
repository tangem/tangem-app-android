package com.tangem.features.onramp.hottokens

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Hot crypto component
 *
[REDACTED_AUTHOR]
 */
internal interface HotCryptoComponent : ComposableContentComponent {

    /**
     * Params
     *
     * @property userWalletId user wallet id
     * @property onTokenClick lambda be invoked when token item is clicked
     */
    data class Params(
        val userWalletId: UserWalletId,
        val onTokenClick: (CryptoCurrencyStatus) -> Unit,
    )

    interface Factory : ComponentFactory<Params, HotCryptoComponent>
}