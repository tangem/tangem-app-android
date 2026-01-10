package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetMultiCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.mapToFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal

class EstimateFeeForTokenUseCase(
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
    private val currenciesRepository: CurrenciesRepository,
    private val getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
) {

    private val tokenFeeCalculator = TokenFeeCalculator(
        walletManagersFacade = walletManagersFacade,
        gaslessTransactionRepository = gaslessTransactionRepository,
        demoConfig = demoConfig,
    )

    suspend operator fun invoke(
        userWallet: UserWallet,
        tokenCurrencyStatus: CryptoCurrencyStatus,
        amount: BigDecimal,
    ): Either<GetFeeError, TransactionFeeExtended> {
        return either {
            val token = tokenCurrencyStatus.currency
            if (!gaslessTransactionRepository.isNetworkSupported(token.network)) {
                raise(GetFeeError.GaslessError.NetworkIsNotSupported)
            }

            val amountData = amount.convertToSdkAmount(tokenCurrencyStatus)
            val result = if (userWallet is UserWallet.Cold &&
                demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)
            ) {
                demoTransactionSender(userWallet, token).estimateFee(
                    amount = amountData,
                    destination = "",
                )
            } else {
                walletManagersFacade.estimateFee(
                    amount = amountData,
                    userWalletId = userWallet.walletId,
                    network = token.network,
                )
            }

            val initialTxFee = when (result) {
                is Result.Success -> result.data
                is Result.Failure -> raise(result.mapToFeeError())
                null -> raise(GetFeeError.UnknownError)
            }

            val initialFeeEth = initialTxFee.normal as? Fee.Ethereum
                ?: raiseIllegalStateError(
                    error = "only Fee.Ethereum supported, but was different",
                )

            val nativeCurrency = currenciesRepository.getNetworkCoin(
                userWalletId = userWallet.walletId,
                networkId = token.network.id,
                derivationPath = token.network.derivationPath,
            )

            val userCurrenciesStatusesByNetwork = getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                userWallet.walletId,
            ).getOrNull()?.filter {
                it.currency.network.id == token.network.id
            } ?: raiseIllegalStateError("currencies list is null for userWalletId=${userWallet.walletId}")

            val nativeCurrencyStatus = userCurrenciesStatusesByNetwork.find {
                it.currency.id == nativeCurrency.id
            } ?: raiseIllegalStateError("native currency not found for network ${token.network.id}")

            val walletManager = prepareWalletManager(userWallet, token.network)

            tokenFeeCalculator.calculateTokenFee(
                walletManager = walletManager,
                tokenForPayFeeStatus = tokenCurrencyStatus,
                nativeCurrencyStatus = nativeCurrencyStatus,
                initialFee = initialFeeEth,
            ).bind()
        }
    }

    @Suppress("NullableToStringCall")
    private suspend fun Raise<GetFeeError>.prepareWalletManager(
        userWallet: UserWallet,
        network: Network,
    ): EthereumWalletManager {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWallet.walletId,
            network = network,
        )
        val ethereumWalletManager = walletManager as? EthereumWalletManager
            ?: raiseIllegalStateError("WalletManager type ${walletManager?.javaClass?.name} not supported")
        return ethereumWalletManager
    }

    private suspend fun demoTransactionSender(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): DemoTransactionSender {
        return DemoTransactionSender(
            walletManagersFacade
                .getOrCreateWalletManager(userWallet.walletId, cryptoCurrency.network)
                ?: error("WalletManager is null"),
        )
    }
}