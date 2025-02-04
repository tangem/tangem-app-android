package com.tangem.data.onramp

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.onramp.converters.HotCryptoCurrencyConverter
import com.tangem.datasource.exchangeservice.hotcrypto.HotCryptoResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.onramp.repositories.HotCryptoRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Default implementation of [HotCryptoRepository]
 *
 * @property excludedBlockchains    excluded blockchains
 * @property hotCryptoResponseStore store of `HotCryptoResponse`
 * @property userWalletsStore       store of `UserWallet`
 *
[REDACTED_AUTHOR]
 */
internal class DefaultHotCryptoRepository(
    private val excludedBlockchains: ExcludedBlockchains,
    private val hotCryptoResponseStore: HotCryptoResponseStore,
    private val userWalletsStore: UserWalletsStore,
) : HotCryptoRepository {

    override fun getCurrencies(userWalletId: UserWalletId): Flow<List<HotCryptoCurrency>> {
        return hotCryptoResponseStore.get()
            .map {
                val userWallet = userWalletsStore.getSyncOrNull(userWalletId)
                    ?: error("UserWalletId [$userWalletId] not found")

                HotCryptoCurrencyConverter(
                    scanResponse = userWallet.scanResponse,
                    imageHost = it.imageHost,
                    excludedBlockchains = excludedBlockchains,
                ).convertList(input = it.tokens)
            }
    }
}