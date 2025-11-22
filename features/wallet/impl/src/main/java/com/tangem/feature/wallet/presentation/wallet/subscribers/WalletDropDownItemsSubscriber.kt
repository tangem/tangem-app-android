package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWalletCardDropDownItemsTransformer
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

internal class WalletDropDownItemsSubscriber(
    private val stateHolder: WalletStateController,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val clickIntents: WalletClickIntents,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) : WalletSubscriber() {
    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        if (!hotWalletFeatureToggles.isHotWalletEnabled) {
            return shouldSaveUserWalletsUseCase.invoke()
                .distinctUntilChanged()
                .onEach { shouldSaveUserWallets ->
                    stateHolder.update(
                        SetWalletCardDropDownItemsTransformer(
                            dropdownEnabled = shouldSaveUserWallets,
                            clickIntents = clickIntents,
                            hotWalletFeatureToggles = hotWalletFeatureToggles,
                        ),
                    )
                }
        } else {
            return flow<Unit> {
                stateHolder.update(
                    SetWalletCardDropDownItemsTransformer(
                        dropdownEnabled = true,
                        clickIntents = clickIntents,
                        hotWalletFeatureToggles = hotWalletFeatureToggles,
                    ),
                )
            }
        }
    }
}