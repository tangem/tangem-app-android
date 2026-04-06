package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.ui.DesignFeatureToggles
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
    private val multiWalletWarningsSubscriberFactory: MultiWalletWarningsSubscriber.Factory,
    private val walletNotificationsSubscriberFactory: WalletNotificationsSubscriber.Factory,
    private val multiWalletActionButtonsSubscriberFactory: MultiWalletActionButtonsSubscriber.Factory,
    private val tangemPayMainSubscriberFactory: TangemPayMainSubscriber.Factory,
    private val tokenListAnalyticsSubscriberFactory: TokenListAnalyticsSubscriber.Factory,
    private val designFeatureToggles: DesignFeatureToggles,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOf(
        accountListSubscriberFactory.create(userWallet),
        walletNFTListSubscriberFactory.create(userWallet),
        checkWalletWithFundsSubscriberFactory.create(userWallet),
        if (designFeatureToggles.isRedesignEnabled) {
            walletNotificationsSubscriberFactory.create(userWallet)
        } else {
            multiWalletWarningsSubscriberFactory.create(userWallet)
        },
        multiWalletActionButtonsSubscriberFactory.create(userWallet),
        tangemPayMainSubscriberFactory.create(userWallet),
        tokenListAnalyticsSubscriberFactory.create(userWallet),
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): MultiWalletContentLoader
    }
}