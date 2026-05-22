package com.tangem.data.pay.entity

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.requireUserWalletsSync
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TangemPayCurrencyFactory @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val networkFactory: NetworkFactory,
) {
    private val cryptoCurrencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        CryptoCurrencyFactory(excludedBlockchains)
    }

    fun create(userWalletId: UserWalletId): CryptoCurrency.Coin {
        val userWallet = userWalletsListRepository.requireUserWalletsSync()
            .firstOrNull { it.walletId == userWalletId }
            ?: error("User wallet with id $userWalletId not found")
        val network = networkFactory.create(
            blockchain = VisaUtilities.visaBlockchain,
            userWallet = userWallet,
            extraDerivationPath = null,
        )
        return cryptoCurrencyFactory.createCoin(requireNotNull(network))
    }

    companion object {
        internal const val TOKEN_ID = "usd-coin"
        internal const val TOKEN_NAME = "USDC"
        internal const val TOKEN_CONTRACT_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
        internal const val TOKEN_DECIMALS = 6
    }
}