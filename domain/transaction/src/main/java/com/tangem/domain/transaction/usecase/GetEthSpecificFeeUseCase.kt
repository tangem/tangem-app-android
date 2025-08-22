package com.tangem.domain.transaction.usecase

import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWallet
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/**
 * Use case to get transaction fee for ETH when we have gas from other services
 *
 */
class GetEthSpecificFeeUseCase(
    private val walletManagersFacade: WalletManagersFacade,
) {
    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        gasLimit: BigInteger,
        gasPrice: BigInteger? = null,
    ) = either {
        catch(
            block = {
                val walletManager = walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWallet.walletId,
                    network = cryptoCurrency.network,
                )
                val gasPriceResult = gasPrice
                    ?: (walletManager as? EthereumWalletManager)?.getGasPriceValue()
                    ?: error("not supported for ${cryptoCurrency.network}")

                val blockchain = Blockchain.fromNetworkId(networkId = cryptoCurrency.network.backendId)
                    ?: error("unknown networkId ${cryptoCurrency.network.backendId}")

                val minimalFee = getEthLegacyFee(
                    gasPrice = gasPriceResult,
                    gasLimit = gasLimit,
                    decimals = cryptoCurrency.decimals,
                    blockchain = blockchain,
                )

                val normalGasPrice = gasPriceResult.increaseBigIntegerByPercents(MULTIPLIER_GAS_PRICE_FOR_NORMAL_FEE)
                val normalFee = getEthLegacyFee(
                    gasPrice = normalGasPrice,
                    gasLimit = gasLimit,
                    decimals = cryptoCurrency.decimals,
                    blockchain = blockchain,
                )

                val priorityGasPrice = gasPriceResult.increaseBigIntegerByPercents(
                    MULTIPLIER_GAS_PRICE_FOR_PRIORITY_FEE,
                )
                val priorityFee = getEthLegacyFee(
                    gasPrice = priorityGasPrice,
                    gasLimit = gasLimit,
                    decimals = cryptoCurrency.decimals,
                    blockchain = blockchain,
                )

                TransactionFee.Choosable(
                    minimum = minimalFee,
                    normal = normalFee,
                    priority = priorityFee,
                )
            },
            catch = {
                raise(GetFeeError.DataError(it))
            },
        )
    }

    private fun getEthLegacyFee(
        gasPrice: BigInteger,
        decimals: Int,
        gasLimit: BigInteger,
        blockchain: Blockchain,
    ): Fee.Ethereum.Legacy {
        val amount = Amount(
            value = gasLimit.multiply(gasPrice).toBigDecimal(
                scale = decimals,
                mathContext = MathContext(decimals, RoundingMode.HALF_EVEN),
            ),
            blockchain = blockchain,
        )
        return Fee.Ethereum.Legacy(
            amount = amount,
            gasLimit = gasLimit,
            gasPrice = gasPrice,
        )
    }

    private suspend fun EthereumWalletManager.getGasPriceValue(): BigInteger {
        return when (val gasPrice = this.getGasPrice()) {
            is Result.Failure -> throw gasPrice.error
            is Result.Success -> gasPrice.data
        }
    }

    /**
     * Increase big integer by percents
     *
     * @param percents in format 150 -> 50%
     * @return increased value
     */
    private fun BigInteger.increaseBigIntegerByPercents(percents: Int?): BigInteger {
        return if (percents != null && percents != 0) {
            this.multiply(percents.toBigInteger()).divide(BigInteger("100"))
        } else {
            this
        }
    }

    private companion object {
        const val MULTIPLIER_GAS_PRICE_FOR_NORMAL_FEE = 150 // 50%
        const val MULTIPLIER_GAS_PRICE_FOR_PRIORITY_FEE = 200 // 50%
    }
}