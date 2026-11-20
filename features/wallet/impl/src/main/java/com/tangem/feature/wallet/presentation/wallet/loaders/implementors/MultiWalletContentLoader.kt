package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.subscribers.*
import com.tangem.features.wallet.WalletFeatureToggles
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
    private val jointOperationBannerSubscriberFactory: JointOperationBannerSubscriber.Factory,
    private val jointAccountTokensSubscriberFactory: JointAccountTokensSubscriber.Factory,
    private val permanentUpgradeBannerSubscriberFactory: PermanentUpgradeBannerSubscriber.Factory,
    private val walletFeatureToggles: WalletFeatureToggles,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOfNotNull(
        accountListSubscriberFactory.create(userWallet),
        walletNFTListSubscriberFactory.create(userWallet),
        checkWalletWithFundsSubscriberFactory.create(userWallet),
        walletNotificationsSubscriberFactory.create(userWallet),
        multiWalletActionButtonsSubscriberFactory.create(userWallet),
        tangemPayMainSubscriberFactory.create(userWallet),
        tokenListAnalyticsSubscriberFactory.create(userWallet),
        jointOperationBannerSubscriberFactory.create(userWallet),
        jointAccountTokensSubscriberFactory.create(userWallet),
        if (userWallet is UserWallet.Hot) {
            assetsDiscoverySubscriberFactory.create(userWallet)
        } else {
            null
        },
        if (userWallet is UserWallet.Hot && walletFeatureToggles.isPermanentUpgradeBannerEnabled) {
            permanentUpgradeBannerSubscriberFactory.create(userWallet)
        } else {
            null
        },
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): MultiWalletContentLoader
    }
}