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
    private val walletNFTListSubscriberFactory: WalletNFTListSubscriberV2.Factory,
    private val checkWalletWithFundsSubscriberFactory: CheckWalletWithFundsSubscriber.Factory,
    private val multiWalletWarningsSubscriberFactory: MultiWalletWarningsSubscriber.Factory,
    private val multiWalletActionButtonsSubscriberFactory: MultiWalletActionButtonsSubscriber.Factory,
    private val tangemPayMainSubscriberFactory: TangemPayMainSubscriber.Factory,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOf(
        accountListSubscriberFactory.create(userWallet),
        walletNFTListSubscriberFactory.create(userWallet),
        checkWalletWithFundsSubscriberFactory.create(userWallet),
        multiWalletWarningsSubscriberFactory.create(userWallet),
        multiWalletActionButtonsSubscriberFactory.create(userWallet),
        tangemPayMainSubscriberFactory.create(userWallet),
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): MultiWalletContentLoader
    }
}