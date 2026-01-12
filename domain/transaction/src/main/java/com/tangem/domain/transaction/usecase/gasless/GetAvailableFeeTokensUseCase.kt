package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetMultiCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.raiseIllegalStateError
import java.math.BigDecimal

class GetAvailableFeeTokensUseCase(
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    private val currencyChecksRepository: CurrencyChecksRepository,
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
                    val userCurrenciesStatuses = getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                        userWallet.walletId,
                    ).getOrNull()
                        ?: raiseIllegalStateError("currencies list is null for userWalletId=${userWallet.walletId}")

                    val nativeCurrencyStatus = getNativeCurrencyStatus(userWallet, network, userCurrenciesStatuses)

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
                    raise(GetFeeError.DataError(it))
                },
            )
        }
    }

    private suspend fun Raise<GetFeeError>.getNativeCurrencyStatus(
        userWallet: UserWallet,
        network: Network,
        userCurrenciesStatuses: List<CryptoCurrencyStatus>,
    ): CryptoCurrencyStatus {
        val nativeCurrency = currenciesRepository.getNetworkCoin(
            userWalletId = userWallet.walletId,
            networkId = network.id,
            derivationPath = network.derivationPath,
        )
        return userCurrenciesStatuses.find {
            it.currency.id == nativeCurrency.id
        } ?: raiseIllegalStateError("no native currency found")
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
            .filter { it.currency.network.id == network.id }
            .filter { currencyStatus ->
                val token = currencyStatus.currency
                token is CryptoCurrency.Token &&
                    currencyStatus.value.amount?.let { amount -> amount > BigDecimal.ZERO } == true &&
                    supportedGaslessTokens.contains(token.contractAddress.lowercase())
            }
            .toList()
    }
}