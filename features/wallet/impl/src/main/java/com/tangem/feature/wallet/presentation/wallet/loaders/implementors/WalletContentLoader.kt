package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber

/**
 * Wallet content loader
 *
 * @property id loader id
 *
[REDACTED_AUTHOR]
 */
internal abstract class WalletContentLoader(val id: UserWalletId) {

    /** Loader's subscribers */
    val subscribers: List<WalletSubscriber> get() = create()

    protected abstract fun create(): List<WalletSubscriber>
}