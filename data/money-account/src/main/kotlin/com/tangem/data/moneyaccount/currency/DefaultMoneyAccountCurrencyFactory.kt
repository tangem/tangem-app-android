package com.tangem.data.moneyaccount.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.domain.moneyaccount.derivation.MONEY_ACCOUNT_DERIVATION_PATH
import com.tangem.domain.pay.TangemPayCurrencyFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultMoneyAccountCurrencyFactory @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val networkFactory: NetworkFactory,
) : MoneyAccountCurrencyFactory {

    private val cryptoCurrencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        CryptoCurrencyFactory(excludedBlockchains)
    }

    override fun create(userWalletId: UserWalletId): CryptoCurrency.Token {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val network = networkFactory.create(
            blockchain = Blockchain.Polygon,
            derivationPath = Network.DerivationPath.Custom(MONEY_ACCOUNT_DERIVATION_PATH.rawPath),
            userWallet = userWallet,
        )
        return cryptoCurrencyFactory.createToken(
            network = requireNotNull(network),
            rawId = TangemPayCurrencyFactory.TOKEN_ID,
            name = TangemPayCurrencyFactory.TOKEN_NAME,
            symbol = TangemPayCurrencyFactory.TOKEN_NAME,
            contractAddress = TangemPayCurrencyFactory.TOKEN_CONTRACT_ADDRESS,
            decimals = TangemPayCurrencyFactory.TOKEN_DECIMALS,
        )
    }
}