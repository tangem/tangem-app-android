package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCoinStatus
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal

@Suppress("LongParameterList")
class EstimateFeeForTokenUseCase(
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val gaslessYieldRepository: GaslessYieldRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
    private val isYieldWithdrawEnabled: Boolean,
) {

    private val tokenFeeCalculator = TokenFeeCalculator(
        walletManagersFacade = walletManagersFacade,
        gaslessTransactionRepository = gaslessTransactionRepository,
        demoConfig = demoConfig,
        gaslessYieldRepository = gaslessYieldRepository,
    )

    suspend operator fun invoke(
        userWallet: UserWallet,
        feeTokenCurrencyStatus: CryptoCurrencyStatus,
        sendingTokenCurrencyStatus: CryptoCurrencyStatus,
        amount: BigDecimal,
    ): Either<GetFeeError, TransactionFeeExtended> {
        return either {
            catch(
                block = {
                    val token = feeTokenCurrencyStatus.currency
                    if (!currencyChecksRepository.isNetworkSupportedForGaslessTx(token.network)) {
                        raise(GaslessError.NetworkIsNotSupported)
                    }

                    val initialTxFee = tokenFeeCalculator.estimateInitialFee(
                        userWallet = userWallet,
                        amount = amount,
                        txTokenCurrencyStatus = sendingTokenCurrencyStatus,
                    ).bind()

                    val initialFeeEth = initialTxFee.normal as? Fee.Ethereum
                        ?: raiseIllegalStateError(
                            error = "only Fee.Ethereum supported, but was different",
                        )

                    val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWallet.walletId)
                        ?: raiseIllegalStateError("AccountStatusList is null for ${userWallet.walletId}")

                    val nativeCurrencyStatus = accountStatusList.getCoinStatus(token.network).getOrElse {
                        raiseIllegalStateError("No native currency found: ${token.network.id}")
                    }

                    val walletManager = prepareWalletManager(userWallet, token.network)

                    val isYieldActive = isYieldWithdrawEnabled &&
                        feeTokenCurrencyStatus.value.yieldSupplyStatus?.isActive == true

                    val tokenFeeExtended = tokenFeeCalculator.calculateTokenFee(
                        walletManager = walletManager,
                        tokenForPayFeeStatus = feeTokenCurrencyStatus,
                        nativeCurrencyStatus = nativeCurrencyStatus,
                        initialFee = initialFeeEth,
                        isYieldActive = isYieldActive,
                        userWallet = userWallet,
                    ).bind()

                    if (isYieldActive) {
                        attachPlanOrFallBackToNativeFee(
                            userWallet = userWallet,
                            feeTokenCurrencyStatus = feeTokenCurrencyStatus,
                            sendingTokenCurrencyStatus = sendingTokenCurrencyStatus,
                            tokenFeeExtended = tokenFeeExtended,
                            amount = amount,
                            nativeFeeResult = TransactionFeeExtended(
                                transactionFee = initialTxFee,
                                feeTokenId = nativeCurrencyStatus.currency.id,
                            ),
                        ).copy(nativeFee = initialTxFee)
                    } else {
                        tokenFeeExtended.copy(nativeFee = initialTxFee)
                    }
                },
                catch = {
                    raise(GaslessError.DataError(it))
                },
            )
        }
    }

    @Suppress("LongParameterList")
    private suspend fun Raise<GetFeeError>.attachPlanOrFallBackToNativeFee(
        userWallet: UserWallet,
        feeTokenCurrencyStatus: CryptoCurrencyStatus,
        sendingTokenCurrencyStatus: CryptoCurrencyStatus,
        tokenFeeExtended: TransactionFeeExtended,
        amount: BigDecimal,
        nativeFeeResult: TransactionFeeExtended,
    ): TransactionFeeExtended {
        val feeTokenContract = (feeTokenCurrencyStatus.currency as? CryptoCurrency.Token)?.contractAddress
            ?: raiseIllegalStateError("gasless fee currency must be a token")

        return either {
            attachGaslessFeePlan(
                resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
                userWallet = userWallet,
                tokenStatus = feeTokenCurrencyStatus,
                tokenFeeExtended = tokenFeeExtended,
                sendAmountInFeeToken = computeSendAmountInFeeToken(
                    sendingCurrency = sendingTokenCurrencyStatus.currency,
                    feeTokenContract = feeTokenContract,
                    amount = amount,
                ),
                isYieldActive = true,
            )
        }.getOrElse { error ->
            when (error) {
                // Liquid + module balance cannot cover send + fee (e.g. the user swapped MAX), or the yield
                // balance could not be read. Mirrors the auto path in [EstimateFeeForGaslessTxUseCase]:
                // return the native fee so the block still renders and the caller can show "not enough funds",
                // instead of failing the whole fee load and leaving an empty block.
                GaslessError.NotEnoughFunds,
                GaslessError.YieldBalanceUnavailable,
                -> nativeFeeResult
                else -> raise(error)
            }
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