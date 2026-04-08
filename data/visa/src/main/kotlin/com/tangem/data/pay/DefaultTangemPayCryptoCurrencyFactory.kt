package com.tangem.data.pay

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.error.UniversalError
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.pay.entity.TangemPayCurrencyFactory
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayCryptoCurrencyFactory
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

private const val TAG = "TangemPay: DefaultTangemPayCryptoCurrencyFactory"

@Deprecated("Use TangemPayCurrencyFactory instead")
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
                rawId = CryptoCurrency.RawID(TangemPayCurrencyFactory.TOKEN_ID),
                name = TangemPayCurrencyFactory.TOKEN_NAME,
                symbol = TangemPayCurrencyFactory.TOKEN_NAME,
                contractAddress = TangemPayCurrencyFactory.TOKEN_CONTRACT_ADDRESS,
                decimals = TangemPayCurrencyFactory.TOKEN_DECIMALS,
            )
        }.mapLeft { exception ->
            TangemLogger.withTag(TAG).e("Error", exception)
            errorConverter.convert(exception)
        }
    }
}