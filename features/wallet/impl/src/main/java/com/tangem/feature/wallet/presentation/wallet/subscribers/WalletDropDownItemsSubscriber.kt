package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWalletCardDropDownItemsTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class WalletDropDownItemsSubscriber(
    private val stateHolder: WalletStateController,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val clickIntents: WalletClickIntents,
) : WalletSubscriber() {
    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return flow<Any> {
            shouldSaveUserWalletsUseCase.invoke()
                .distinctUntilChanged()
                .onEach {
                    stateHolder.update(
                        SetWalletCardDropDownItemsTransformer(
                            dropdownEnabled = it,
                            clickIntents = clickIntents,
                        ),
                    )
                }
                .launchIn(coroutineScope)
        }
    }
}