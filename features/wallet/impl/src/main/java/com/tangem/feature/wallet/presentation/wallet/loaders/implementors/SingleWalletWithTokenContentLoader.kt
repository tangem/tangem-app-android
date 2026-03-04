package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.subscribers.CheckWalletWithFundsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.MultiWalletWarningsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.SingleWalletWithTokenSubscriberLegacy
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Deprecated("Remove with main toggle [DesignFeatureToggles.isRedesignEnabled]")
internal class SingleWalletWithTokenContentLoader @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet.Cold,
    private val singleWalletWithTokenSubscriberLegacyFactory: SingleWalletWithTokenSubscriberLegacy.Factory,
    private val multiWalletWarningsSubscriberFactory: MultiWalletWarningsSubscriber.Factory,
    private val checkWalletWithFundsSubscriberFactory: CheckWalletWithFundsSubscriber.Factory,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOf(
        singleWalletWithTokenSubscriberLegacyFactory.create(userWallet),
        multiWalletWarningsSubscriberFactory.create(userWallet),
        checkWalletWithFundsSubscriberFactory.create(userWallet),
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet.Cold): SingleWalletWithTokenContentLoader
    }
}