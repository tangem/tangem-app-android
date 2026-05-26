package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.stories.GetStoryContentUseCase
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWarningsTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@Deprecated("Remove with main toggle [DesignFeatureToggles.isRedesignEnabled]")
internal class MultiWalletWarningsSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val getStoryContentUseCase: GetStoryContentUseCase,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<ImmutableList<WalletNotification>> {
        return getMultiWalletWarningsFactory.create(userWallet, clickIntents)
            .conflate()
            .distinctUntilChanged()
            .onEach { warnings ->
                if (warnings.any { it is WalletNotification.YieldBoostPromo }) {
                    coroutineScope.launch {
                        getStoryContentUseCase.invokeSync(
                            id = StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id,
                            refresh = true,
                        )
                    }
                }

                val displayedState = stateController.getWalletState(userWallet.walletId)

                // Wait until the wallet appears in the list
                stateController.uiState.first {
                    it.wallets.any { walletState -> walletState.walletCardState.id == userWallet.walletId }
                }

                stateController.update(
                    SetWarningsTransformer(
                        userWalletId = userWallet.walletId,
                        warnings = warnings,
                        notifications = persistentListOf(),
                    ),
                )
                walletWarningsAnalyticsSender.send(displayedState, warnings)
                walletWarningsSingleEventSender.send(
                    userWalletId = userWallet.walletId,
                    displayedUiState = displayedState,
                    newWarnings = warnings,
                )
            }
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): MultiWalletWarningsSubscriber
    }
}