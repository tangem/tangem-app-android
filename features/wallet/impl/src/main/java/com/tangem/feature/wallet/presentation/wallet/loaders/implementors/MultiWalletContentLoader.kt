package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.subscribers.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class MultiWalletContentLoader @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val accountListSubscriberFactory: AccountListSubscriber.Factory,
    private val walletNFTListSubscriberFactory: WalletNFTListSubscriber.Factory,
    private val checkWalletWithFundsSubscriberFactory: CheckWalletWithFundsSubscriber.Factory,
    private val walletNotificationsSubscriberFactory: WalletNotificationsSubscriber.Factory,
    private val multiWalletActionButtonsSubscriberFactory: MultiWalletActionButtonsSubscriber.Factory,
    private val tangemPayMainSubscriberFactory: TangemPayMainSubscriber.Factory,
    private val assetsDiscoverySubscriberFactory: AssetsDiscoverySubscriber.Factory,
    private val tokenListAnalyticsSubscriberFactory: TokenListAnalyticsSubscriber.Factory,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOfNotNull(
        accountListSubscriberFactory.create(userWallet),
        walletNFTListSubscriberFactory.create(userWallet),
        checkWalletWithFundsSubscriberFactory.create(userWallet),
        walletNotificationsSubscriberFactory.create(userWallet),
        multiWalletActionButtonsSubscriberFactory.create(userWallet),
        tangemPayMainSubscriberFactory.create(userWallet),
        tokenListAnalyticsSubscriberFactory.create(userWallet),
        if (userWallet is UserWallet.Hot) {
            assetsDiscoverySubscriberFactory.create(userWallet)
        } else {
            null
        },
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): MultiWalletContentLoader
    }
}