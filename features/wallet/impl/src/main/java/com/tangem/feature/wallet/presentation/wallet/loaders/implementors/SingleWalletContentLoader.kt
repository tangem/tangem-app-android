package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.subscribers.CheckWalletWithFundsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.SingleWalletSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletNotificationsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * This content loader is used for the wallet screen when for single wallets.
 * For example - [Note, Twins, Single with token]
 */
internal class SingleWalletContentLoader @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet.Cold,
    private val walletNotificationsSubscriber: WalletNotificationsSubscriber.Factory,
    private val singleWalletSubscriber: SingleWalletSubscriber.Factory,
    private val checkWalletWithFundsSubscriberFactory: CheckWalletWithFundsSubscriber.Factory,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOf(
        singleWalletSubscriber.create(userWallet = userWallet),
        walletNotificationsSubscriber.create(userWallet = userWallet),
        checkWalletWithFundsSubscriberFactory.create(userWallet = userWallet),
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet.Cold): SingleWalletContentLoader
    }
}