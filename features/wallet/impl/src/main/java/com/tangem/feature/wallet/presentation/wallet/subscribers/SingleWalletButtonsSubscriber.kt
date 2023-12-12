package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.collectLatest
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetCryptoCurrencyActionsTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class SingleWalletButtonsSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateHolderV2,
    private val clickIntents: WalletClickIntentsV2,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<TokenActionsState> {
        return channelFlow {
            getPrimaryCurrencyStatusUpdatesUseCase.collectLatest(userWalletId = userWallet.walletId) { status ->
                getCryptoCurrencyActionsUseCase(userWallet = userWallet, cryptoCurrencyStatus = status)
                    .conflate()
                    .distinctUntilChanged()
                    .firstOrNull()
                    ?.let { send(it) }
            }
        }
            .onEach(::updateContent)
    }

    private fun updateContent(tokenActionsState: TokenActionsState) {
        stateHolder.update(
            SetCryptoCurrencyActionsTransformer(
                tokenActionsState = tokenActionsState,
                userWallet = userWallet,
                clickIntents = clickIntents,
            ),
        )
    }
}
