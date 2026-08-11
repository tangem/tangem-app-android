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
import com.tangem.domain.transaction.models.AvailableFeeTokens
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.coroutines.runSuspendCatching
import java.math.BigDecimal

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
     * @param nativeFeeAmount fee the transaction would cost when paid in the native coin, as reported by
     * [com.tangem.domain.transaction.models.TransactionFeeExtended.nativeFee]. When it is known and the
     * native balance cannot cover it, the native coin is reported as not enough for the fee — paying with
     * it would fail anyway. It is still offered normally when no token can pay the fee either.
     *
     * @return the offered currencies, the native one first, along with those that cannot cover the fee
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        network: Network,
        nativeFeeAmount: BigDecimal? = null,
    ): Either<GetFeeError, AvailableFeeTokens> {
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
                    // gasless backend, not the EVM gasless service. Its fee is a backend compensation
                    // quote rather than a gas estimation, so the native-coin filter below is not applied
                    // here — TRX cost depends on the account's bandwidth/energy and an estimation can
                    // overshoot what is actually charged.
                    if (isTron(network.rawId)) {
                        return@either AvailableFeeTokens(
                            tokens = buildList {
                                add(nativeCurrencyStatus)
                                addAll(getTronGaslessTokens(network, userCurrenciesStatuses))
                            },
                        )
                    }

                    if (!currencyChecksRepository.isNetworkSupportedForGaslessTx(network)) {
                        return@either AvailableFeeTokens(tokens = listOf(nativeCurrencyStatus))
                    }

                    buildFeeTokens(
                        nativeCurrencyStatus = nativeCurrencyStatus,
                        nativeFeeAmount = nativeFeeAmount,
                        tokens = getGaslessTokens(network, userCurrenciesStatuses),
                    )
                },
                catch = {
                    raise(GetFeeError.GaslessError.DataError(it))
                },
            )
        }
    }

    private fun buildFeeTokens(
        nativeCurrencyStatus: CryptoCurrencyStatus,
        nativeFeeAmount: BigDecimal?,
        tokens: List<CryptoCurrencyStatus>,
    ): AvailableFeeTokens {
        val isNativeNotEnough = tokens.any(::hasSpendableBalance) &&
            !canPayFee(status = nativeCurrencyStatus, feeAmount = nativeFeeAmount)

        return AvailableFeeTokens(
            tokens = buildList {
                add(nativeCurrencyStatus)
                addAll(tokens)
            },
            notEnoughForFeeIds = if (isNativeNotEnough) setOf(nativeCurrencyStatus.currency.id) else emptySet(),
        )
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
        val supportedContracts = runSuspendCatching { tronGaslessTransactionRepository.getSupportedTokens() }
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

        private fun hasSpendableBalance(status: CryptoCurrencyStatus): Boolean {
            if (status.value.yieldSupplyStatus?.isActive == true) return true
            val amount = status.value.amount ?: return false
            return amount.signum() > 0
        }

        private fun canPayFee(status: CryptoCurrencyStatus, feeAmount: BigDecimal?): Boolean {
            if (feeAmount == null) return true
            val balance = status.value.amount ?: return true
            return balance >= feeAmount
        }
    }
}