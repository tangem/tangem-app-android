package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWalletCardDropDownItemsTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

internal class WalletDropDownItemsSubscriber(
    private val stateHolder: WalletStateController,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val clickIntents: WalletClickIntents,
) : WalletSubscriber() {
    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return shouldSaveUserWalletsUseCase.invoke()
            .distinctUntilChanged()
            .onEach {
                stateHolder.update(
                    SetWalletCardDropDownItemsTransformer(
                        dropdownEnabled = it,
                        clickIntents = clickIntents,
                    ),
                )
            }
    }
}