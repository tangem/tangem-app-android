package com.tangem.data.pay

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.error.UniversalError
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.data.pay.util.TangemPayWalletsManager
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.ReceiveAddressModel.NameService
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.pay.DataForReceive
import com.tangem.domain.pay.DataForReceiveFactory
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "TangemPay: TokenReceiveConfigFactory"

internal class DefaultDataForReceiveFactory @Inject constructor(
    @NetworkMoshi moshi: Moshi,
    private val tangemPayWalletsManager: TangemPayWalletsManager,
    excludedBlockchains: ExcludedBlockchains,
) : DataForReceiveFactory {

    private val cryptoCurrencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        CryptoCurrencyFactory(excludedBlockchains)
    }
    private val errorConverter by lazy(mode = LazyThreadSafetyMode.NONE) { TangemPayErrorConverter(moshi) }

    override fun getDataForReceive(depositAddress: String, chainId: Int): Either<UniversalError, DataForReceive> {
        return try {
            val wallet = tangemPayWalletsManager.getDefaultWalletForTangemPayBlocking()

            /**
             * Create [CryptoCurrency.Coin] only for F&F.
             * Later will use [CryptoCurrency.Token] when contractAddresses will be provided by BFF.
             */
            val currency = cryptoCurrencyFactory.createCoin(
                chainId = chainId,
                extraDerivationPath = null,
                userWallet = wallet,
            ) ?: error("Cannot create crypto currency from chainId $chainId")

            val result = DataForReceive(
                currency = currency,
                walletId = wallet.walletId,
                receiveAddress = listOf(ReceiveAddressModel(nameService = NameService.Default, value = depositAddress)),
            )

            Either.Right(result)
        } catch (exception: Exception) {
            when (exception) {
                is CancellationException -> {
                    throw exception
                }
                else -> {
                    Timber.tag(TAG).e(exception)
                    Either.Left(errorConverter.convert(exception))
                }
            }
        }
    }
}