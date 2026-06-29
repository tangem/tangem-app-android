package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCoinStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.lib.crypto.BlockchainUtils.isTron

class GetAvailableFeeTokensUseCase(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val tronGaslessTransactionRepository: TronGaslessTransactionRepository,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val isYieldWithdrawEnabled: Boolean,
) {

    /**
     * Retrieves available tokens for gasless fee payment.
     *
     * @return List where first element is always native currency (for fallback)
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        network: Network,
    ): Either<GetFeeError, List<CryptoCurrencyStatus>> {
        return either {
            catch(
                block = {
                    val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWallet.walletId)
                        ?: raiseIllegalStateError("AccountStatusList is null for ${userWallet.walletId}")

                    val userCurrenciesStatuses = accountStatusList.flattenCurrencies()

                    val nativeCurrencyStatus = accountStatusList.getCoinStatus(network).getOrElse {
                        raiseIllegalStateError("No native currency found: ${network.id}")
                    }

                    // Tron gasless is a parallel path: its supported fee tokens come from the Tron
                    // gasless backend, not the EVM gasless service.
                    if (isTron(network.rawId)) {
                        return@either buildList {
                            add(nativeCurrencyStatus)
                            addAll(getTronGaslessTokens(network, userCurrenciesStatuses))
                        }
                    }

                    if (!currencyChecksRepository.isNetworkSupportedForGaslessTx(network)) {
                        return@either listOf(nativeCurrencyStatus)
                    }

                    val gaslessTokens = getGaslessTokens(network, userCurrenciesStatuses)
                    buildList {
                        add(nativeCurrencyStatus)
                        addAll(gaslessTokens)
                    }
                },
                catch = {
                    raise(GetFeeError.GaslessError.DataError(it))
                },
            )
        }
    }

    private suspend fun getGaslessTokens(
        network: Network,
        userCurrenciesStatuses: List<CryptoCurrencyStatus>,
    ): List<CryptoCurrencyStatus> {
        val supportedGaslessTokens = gaslessTransactionRepository.getSupportedTokens(network)
            .mapNotNull {
                (it as? CryptoCurrency.Token)?.contractAddress?.lowercase()
            }.toSet()
        return userCurrenciesStatuses
            .asSequence()
            .filter { isEligibleFeeToken(it, isYieldWithdrawEnabled) }
            .filter { it.currency.network.id == network.id }
            .filter { currencyStatus ->
                val token = currencyStatus.currency
                token is CryptoCurrency.Token && supportedGaslessTokens.contains(token.contractAddress.lowercase())
            }
            .toList()
    }

    private suspend fun getTronGaslessTokens(
        network: Network,
        userCurrenciesStatuses: List<CryptoCurrencyStatus>,
    ): List<CryptoCurrencyStatus> {
        val supportedContracts = runCatching { tronGaslessTransactionRepository.getSupportedTokens() }
            .getOrDefault(emptyList())
            .map { it.contractAddress }
            .toSet()
        return userCurrenciesStatuses.filter { status ->
            val token = status.currency
            token is CryptoCurrency.Token &&
                token.network.id == network.id &&
                supportedContracts.contains(token.contractAddress)
        }
    }

    internal companion object {

        internal fun isEligibleFeeToken(status: CryptoCurrencyStatus, isYieldWithdrawEnabled: Boolean): Boolean {
            val yieldSupplyStatus = status.value.yieldSupplyStatus ?: return true
            return isYieldWithdrawEnabled && yieldSupplyStatus.isActive
        }
    }
}