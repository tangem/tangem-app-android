package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.CheckWalletWithFundsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.MultiWalletWarningsSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.SingleWalletWithTokenSubscriber
import com.tangem.feature.wallet.presentation.wallet.subscribers.WalletSubscriber
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class SingleWalletWithTokenContentLoaderV2 @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet.Cold,
    private val singleWalletWithTokenSubscriberFactory: SingleWalletWithTokenSubscriber.Factory,
    private val checkWalletWithFundsSubscriberFactory: CheckWalletWithFundsSubscriber.Factory,
    private val clickIntents: WalletClickIntents,
    private val stateController: WalletStateController,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOf(
        singleWalletWithTokenSubscriberFactory.create(userWallet),
        MultiWalletWarningsSubscriber(
            userWallet = userWallet,
            stateHolder = stateController,
            clickIntents = clickIntents,
            getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
            walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
            walletWarningsSingleEventSender = walletWarningsSingleEventSender,
        ),
        checkWalletWithFundsSubscriberFactory.create(userWallet),
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet.Cold): SingleWalletWithTokenContentLoaderV2
    }
}