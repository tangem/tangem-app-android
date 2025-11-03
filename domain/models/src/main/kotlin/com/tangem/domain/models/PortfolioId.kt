package com.tangem.domain.models

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * Temporary wrapper over ID to support a gradual migration between two modes:
 *
 * - [Wallet] — legacy flow, wallet design; used when the [AccountsFeatureToggles] is disabled.
 * - [Account] — new flow, wallet/account design; used when the [AccountsFeatureToggles] is enabled.
 *
 * ⚠️ When an [Account] you must verify the current app mode with [IsAccountsModeEnabledUseCase]
 *      and then use wallet/account design
 *
 * Intended to be removed after the full migration to the new mode.
 */
@Serializable
sealed interface PortfolioId {
    val userWalletId: UserWalletId
    val stringValue: String

    @Serializable
    data class Wallet(override val userWalletId: UserWalletId) : PortfolioId {
        override val stringValue: String = userWalletId.stringValue
    }

    @Serializable
    data class Account(val accountId: AccountId) : PortfolioId {
        override val userWalletId: UserWalletId get() = accountId.userWalletId
        override val stringValue: String = accountId.value
    }

    companion object {
        operator fun invoke(accountId: AccountId) = Account(accountId)
        operator fun invoke(userWalletId: UserWalletId) = Wallet(userWalletId)
    }
}