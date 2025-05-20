package com.tangem.data.onramp.legacy

import android.net.Uri
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.calculateSha512
import com.tangem.common.extensions.toHexString
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.repositories.LegacyTopUpRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class MercuryoTopUpRepository @Inject constructor(
    private val environmentConfigStorage: EnvironmentConfigStorage,
    private val dispatchersProvider: CoroutineDispatcherProvider,
) : LegacyTopUpRepository {

    override suspend fun getTopUpUrl(cryptoCurrency: CryptoCurrency, walletAddress: String): String =
        withContext(dispatchersProvider.default) {
            val blockchain = Blockchain.fromId(cryptoCurrency.network.rawId)
            val environmentConfig = environmentConfigStorage.getConfigSync()

            val builder = Uri.Builder()
                .scheme(LegacyTopUpRepository.SCHEME)
                .authority("exchange.mercuryo.io")
                .appendQueryParameter("widget_id", environmentConfig.mercuryoWidgetId)
                .appendQueryParameter("type", "buy")
                .appendQueryParameter("currency", cryptoCurrency.symbol)
                .appendQueryParameter("address", walletAddress)
                .appendQueryParameter("signature", environmentConfig.signature(walletAddress))
                .appendQueryParameter("fix_currency", "true")
                .appendQueryParameter("redirect_url", LegacyTopUpRepository.SUCCESS_URL)

            blockchain.mercuryoNetwork?.let {
                builder.appendQueryParameter("network", it)
            }

            builder.build().toString()
        }

    private fun EnvironmentConfig.signature(address: String) = (address + mercuryoSecret)
        .calculateSha512()
        .toHexString()
        .lowercase()
}