package com.tangem.feature.wallet.presentation.wallet.loaders.implementors

import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletStackableNotificationsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.subscribers.*
import com.tangem.features.tangempay.TangemPayFeatureToggles
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class MultiWalletContentLoaderV2 @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val accountListSubscriberFactory: AccountListSubscriber.Factory,
    private val walletNFTListSubscriberV2Factory: WalletNFTListSubscriberV2.Factory,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val getWalletWarningsFactory: GetWalletWarningsFactory,
    private val getWalletStackableNotificationsFactory: GetWalletStackableNotificationsFactory,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val checkWalletWithFundsSubscriberFactory: CheckWalletWithFundsSubscriber.Factory,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val tangemPayMainSubscriberFactory: TangemPayMainSubscriber.Factory,
    private val designFeatureToggles: DesignFeatureToggles,
) : WalletContentLoader(id = userWallet.walletId) {

    override fun create(): List<WalletSubscriber> = listOfNotNull(
        accountListSubscriberFactory.create(userWallet = userWallet),
        walletNFTListSubscriberV2Factory.create(userWallet = userWallet),
        checkWalletWithFundsSubscriberFactory.create(userWallet = userWallet),
        if (designFeatureToggles.isRedesignEnabled) {
            MultiWalletWarningsSubscriberV2(
                userWallet = userWallet,
                stateHolder = stateController,
                clickIntents = clickIntents,
                getWalletWarningsFactory = getWalletWarningsFactory,
                getWalletStackableNotificationsFactory = getWalletStackableNotificationsFactory,
                walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
                walletWarningsSingleEventSender = walletWarningsSingleEventSender,
            )
        } else {
            MultiWalletWarningsSubscriber(
                userWallet = userWallet,
                stateHolder = stateController,
                clickIntents = clickIntents,
                getMultiWalletWarningsFactory = getMultiWalletWarningsFactory,
                walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
                walletWarningsSingleEventSender = walletWarningsSingleEventSender,
            )
        },
        MultiWalletActionButtonsSubscriber(
            userWallet = userWallet,
            stateHolder = stateController,
            getStoryContentUseCase = getStoryContentUseCase,
        ),

        if (tangemPayFeatureToggles.isTangemPayEnabled) {
            tangemPayMainSubscriberFactory.create(userWallet)
        } else {
            null
        },
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): MultiWalletContentLoaderV2
    }
}