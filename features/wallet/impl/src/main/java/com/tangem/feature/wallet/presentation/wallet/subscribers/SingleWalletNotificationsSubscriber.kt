package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.domain.GetSingleWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetWarningsTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext

/**
[REDACTED_AUTHOR]
 */
internal class SingleWalletNotificationsSubscriber(
    private val userWalletId: UserWalletId,
    private val stateHolder: WalletStateHolderV2,
    private val getSingleWalletWarningsFactory: GetSingleWalletWarningsFactory,
    private val clickIntents: WalletClickIntentsV2,
) : WalletSubscriber<ImmutableList<WalletNotification>>(name = "single_wallet_warnings") {

    override fun create(
        coroutineScope: CoroutineScope,
        uiDispatcher: CoroutineContext,
    ): Flow<ImmutableList<WalletNotification>> {
        return getSingleWalletWarningsFactory.create(clickIntents)
            .conflate()
            .distinctUntilChanged()
            .onEach {
                stateHolder.update(
                    SetWarningsTransformer(userWalletId = userWalletId, warnings = it),
                )
            }
    }
}