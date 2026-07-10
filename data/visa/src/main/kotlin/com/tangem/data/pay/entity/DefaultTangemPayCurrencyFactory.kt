package com.tangem.data.pay.entity

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultTangemPayCurrencyFactory @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val networkFactory: NetworkFactory,
) : TangemPayCurrencyFactory {
    private val cryptoCurrencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        CryptoCurrencyFactory(excludedBlockchains)
    }

    override fun create(userWalletId: UserWalletId): CryptoCurrency.Token {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val network = networkFactory.create(
            blockchain = VisaUtilities.visaBlockchain,
            userWallet = userWallet,
            extraDerivationPath = null,
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

    override fun createVirtualAccountToken(userWalletId: UserWalletId): CryptoCurrency.Token {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val network = networkFactory.create(
            blockchain = VisaUtilities.visaBlockchain,
            derivationPath = Network.DerivationPath.Custom(VisaUtilities.virtualAccountDerivationPath.rawPath),
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