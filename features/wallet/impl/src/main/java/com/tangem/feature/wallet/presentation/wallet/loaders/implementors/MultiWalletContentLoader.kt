package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.subscribers.*
import com.tangem.features.hotwallet.HotWalletFeatureToggles
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
    private val tokenSyncSubscriberFactory: TokenSyncSubscriber.Factory,
    private val tokenListAnalyticsSubscriberFactory: TokenListAnalyticsSubscriber.Factory,
    private val designFeatureToggles: DesignFeatureToggles,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOfNotNull(
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
        if (hotWalletFeatureToggles.isTokenSyncEnabled && userWallet is UserWallet.Hot) {
            tokenSyncSubscriberFactory.create(userWallet)
        } else {
            null
        },
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): MultiWalletContentLoader
    }
}