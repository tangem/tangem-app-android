package com.tangem.data.pay

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.squareup.moshi.Moshi
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.error.UniversalError
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.ReceiveAddressModel.NameService
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayTopUpData
import com.tangem.domain.pay.TangemPaySwapDataFactory
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

private const val TAG = "TangemPay: DefaultDataForTopUpFactory"
/**
 * Custom token parameters. Will be used only for F&F.
 */
private const val TOKEN_ID = "usd-coin"
private const val TOKEN_NAME = "USDC"
private const val TOKEN_CONTRACT_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
private const val TOKEN_DECIMALS = 6

internal class DefaultTangemPaySwapDataFactory @Inject constructor(
    @NetworkMoshi moshi: Moshi,
    excludedBlockchains: ExcludedBlockchains,
) : TangemPaySwapDataFactory {

    private val cryptoCurrencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        CryptoCurrencyFactory(excludedBlockchains)
    }
    private val networkFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        NetworkFactory(excludedBlockchains)
    }
    private val errorConverter by lazy(mode = LazyThreadSafetyMode.NONE) { TangemPayErrorConverter(moshi) }

    private fun getCurrency(userWallet: UserWallet, chainId: Int): CryptoCurrency {
        val chain = requireNotNull(Chain.entries.find { it.id == chainId }) { "Can not find chain with $chainId" }
        val blockchain = requireNotNull(chain.blockchain)
        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = null,
            userWallet = userWallet,
        )
        return cryptoCurrencyFactory.createToken(
            network = requireNotNull(network),
            rawId = CryptoCurrency.RawID(TOKEN_ID),
            name = TOKEN_NAME,
            symbol = TOKEN_NAME,
            contractAddress = TOKEN_CONTRACT_ADDRESS,
            decimals = TOKEN_DECIMALS,
        )
    }

    override fun create(
        userWallet: UserWallet,
        depositAddress: String,
        chainId: Int,
        cryptoBalance: BigDecimal,
        fiatBalance: BigDecimal,
    ): Either<UniversalError, TangemPayTopUpData> {
        return catch {
            val currency = getCurrency(userWallet, chainId)
            TangemPayTopUpData(
                currency = currency,
                walletId = userWallet.walletId,
                cryptoBalance = cryptoBalance,
                fiatBalance = fiatBalance,
                depositAddress = depositAddress,
                receiveAddress = listOf(ReceiveAddressModel(nameService = NameService.Default, value = depositAddress)),
            )
        }.mapLeft { exception ->
            Timber.tag(TAG).e(exception)
            errorConverter.convert(exception)
        }
    }
}