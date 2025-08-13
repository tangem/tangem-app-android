package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import kotlin.reflect.KClass

internal abstract class TypedWalletStateTransformer<S : WalletState>(
    userWalletId: UserWalletId,
    protected val targetStateClass: KClass<S>,
) : WalletStateTransformer(userWalletId) {

    abstract fun transformTyped(prevState: S): WalletState

    @Suppress("UNCHECKED_CAST")
    final override fun transform(prevState: WalletState): WalletState {
        return if (prevState::class == targetStateClass) {
            transformTyped(prevState as S)
        } else {
            prevState
        }
    }
}