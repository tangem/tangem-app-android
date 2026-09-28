package com.tangem.domain.transaction.usecase.gasless

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.coroutines.runSuspendCatching

/**
 * Tells whether the Tron gasless flow is available for the given [network]/[currency].
 *
 * With [isAccountActivationSponsored] the gasless service sponsors the activation of a non-activated
 * account and decides its eligibility when quoting (see [GetTronGaslessFeeUseCase]); without it only an
 * activated account can go gasless.
 */
class IsTronGaslessSupportedUseCase(
    private val repository: TronGaslessTransactionRepository,
    @RemoveWithToggle(
        toggleName = "TWI_1783_TRON_GASLESS_ACCOUNT_ACTIVATION_ENABLED",
        description = "Also drop the activation check from invoke(), leaving only isTokenSupported()",
    )
    private val walletManagersFacade: WalletManagersFacade,
    @RemoveWithToggle(
        toggleName = "TWI_1783_TRON_GASLESS_ACCOUNT_ACTIVATION_ENABLED",
        description = "Also drop the activation check from invoke(), leaving only isTokenSupported()",
    )
    private val isAccountActivationSponsored: () -> Boolean,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network, currency: CryptoCurrency): Boolean {
        if (!isTokenSupported(network = network, currency = currency)) return false
        if (isAccountActivationSponsored()) return true
        return walletManagersFacade.isTronAccountActivated(userWalletId = userWalletId, network = network)
    }

    suspend fun isTokenSupported(network: Network, currency: CryptoCurrency): Boolean {
        if (currency !is CryptoCurrency.Token) return false
        if (!isTron(network.rawId)) return false
        val supported = runSuspendCatching { repository.getSupportedTokens() }.getOrDefault(emptyList())
        return supported.any { it.contractAddress == currency.contractAddress }
    }
}