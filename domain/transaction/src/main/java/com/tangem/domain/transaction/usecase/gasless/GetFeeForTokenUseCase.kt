package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetMultiCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.walletmanager.WalletManagersFacade

class GetFeeForTokenUseCase(
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
    private val currenciesRepository: CurrenciesRepository,
    private val getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    private val currencyChecksRepository: CurrencyChecksRepository,
) {

    private val tokenFeeCalculator = TokenFeeCalculator(
        walletManagersFacade = walletManagersFacade,
        gaslessTransactionRepository = gaslessTransactionRepository,
        demoConfig = demoConfig,
    )

    suspend operator fun invoke(
        userWallet: UserWallet,
        token: CryptoCurrency,
        transactionData: TransactionData,
    ): Either<GetFeeError, TransactionFeeExtended> {
        return either {
            if (!currencyChecksRepository.isNetworkSupportedForGaslessTx(token.network)) {
                raise(GetFeeError.GaslessError.NetworkIsNotSupported)
            }

            val walletManager = prepareWalletManager(userWallet, token.network)

            val initialTxFee = tokenFeeCalculator.calculateInitialFee(
                userWallet = userWallet,
                network = token.network,
                walletManager = walletManager,
                transactionData = transactionData,
            ).bind()

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

            val tokenCurrencyStatus = userCurrenciesStatusesByNetwork.find {
                it.currency.id == token.id
            } ?: raiseIllegalStateError("token currency not found for network ${token.network.id}")

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
}