package com.tangem.data.pay

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.error.UniversalError
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayCryptoCurrencyFactory
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "TangemPay: DefaultTangemPayCryptoCurrencyFactory"
/**
 * Custom token parameters. Will be used only for F&F.
 */
private const val TOKEN_ID = "usd-coin"
private const val TOKEN_NAME = "USDC"
private const val TOKEN_CONTRACT_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
private const val TOKEN_DECIMALS = 6

internal class DefaultTangemPayCryptoCurrencyFactory @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
    private val errorConverter: TangemPayErrorConverter,
) : TangemPayCryptoCurrencyFactory {

    private val cryptoCurrencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        CryptoCurrencyFactory(excludedBlockchains)
    }
    private val networkFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        NetworkFactory(excludedBlockchains)
    }

    override fun create(userWallet: UserWallet, chainId: Int): Either<UniversalError, CryptoCurrency> {
        return catch {
            val chain = requireNotNull(Chain.entries.find { it.id == chainId }) { "Can not find chain with $chainId" }
            val blockchain = requireNotNull(chain.blockchain)
            val network = networkFactory.create(
                blockchain = blockchain,
                extraDerivationPath = null,
                userWallet = userWallet,
            )
            cryptoCurrencyFactory.createToken(
                network = requireNotNull(network),
                rawId = CryptoCurrency.RawID(TOKEN_ID),
                name = TOKEN_NAME,
                symbol = TOKEN_NAME,
                contractAddress = TOKEN_CONTRACT_ADDRESS,
                decimals = TOKEN_DECIMALS,
            )
        }.mapLeft { exception ->
            Timber.tag(TAG).e(exception)
            errorConverter.convert(exception)
        }
    }
}