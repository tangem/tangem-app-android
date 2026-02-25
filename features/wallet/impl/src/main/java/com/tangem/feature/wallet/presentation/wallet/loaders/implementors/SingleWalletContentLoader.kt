package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.subscribers.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class SingleWalletContentLoader @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet.Cold,
    @Assisted private val isRefresh: Boolean,
    private val primaryCurrencySubscriberFactory: PrimaryCurrencySubscriber.Factory,
    private val singleWalletButtonsSubscriberFactory: SingleWalletButtonsSubscriber.Factory,
    private val singleWalletNotificationsSubscriberFactory: SingleWalletNotificationsSubscriber.Factory,
    private val singleWalletExpressStatusesSubscriberFactory: SingleWalletExpressStatusesSubscriber.Factory,
    private val txHistorySubscriberFactory: TxHistorySubscriber.Factory,
    private val checkWalletWithFundsSubscriberFactory: CheckWalletWithFundsSubscriber.Factory,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOf(
        primaryCurrencySubscriberFactory.create(userWallet),
        singleWalletButtonsSubscriberFactory.create(userWallet),
        singleWalletNotificationsSubscriberFactory.create(userWallet),
        singleWalletExpressStatusesSubscriberFactory.create(userWallet),
        txHistorySubscriberFactory.create(userWallet, isRefresh),
        checkWalletWithFundsSubscriberFactory.create(userWallet),
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet.Cold, isRefresh: Boolean): SingleWalletContentLoader
    }
}