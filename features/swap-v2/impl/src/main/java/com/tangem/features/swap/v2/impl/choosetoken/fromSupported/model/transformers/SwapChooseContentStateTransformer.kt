package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.transformers

import com.tangem.core.ui.extensions.iconResId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrency.ID.Companion.MAIN_NETWORK_L2_TYPE_NAME
import com.tangem.domain.models.currency.CryptoCurrency.ID.Companion.MAIN_NETWORK_TYPE_NAME
import com.tangem.domain.models.network.Network
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkContentUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.SwapChooseTokenFactory.getErrorMessage
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class SwapChooseContentStateTransformer(
    private val pairs: SwapCurrencies,
    private val tokenName: String,
    private val onNetworkClick: (SwapCurrencies, CryptoCurrency) -> Unit,
    private val onDismiss: () -> Unit,
) : Transformer<SwapChooseTokenNetworkUM> {
    override fun transform(prevState: SwapChooseTokenNetworkUM): SwapChooseTokenNetworkUM {
        val swapNetworks = pairs.fromGroup.available.map { availableCurrency ->
            val cryptoCurrency = availableCurrency.currencyStatus.currency
            val network = cryptoCurrency.network

            val isMain = cryptoCurrency is CryptoCurrency.Coin
            val subtitle = when {
                BlockchainUtils.isL2Network(networkId = network.backendId) -> MAIN_NETWORK_L2_TYPE_NAME
                isMain -> MAIN_NETWORK_TYPE_NAME
                network.standardType !is Network.StandardType.Unspecified -> network.standardType.name
                else -> ""
            }

            SwapChooseNetworkUM(
                title = stringReference(network.name),
                subtitle = stringReference(subtitle),
                iconResId = network.iconResId,
                isMainNetwork = isMain,
                hasFixedRate = availableCurrency.providers.any { provider ->
                    provider.rateTypes.any { rateType ->
                        rateType == ExpressRateType.Fixed
                    }
                },
                onNetworkClick = { onNetworkClick(pairs, availableCurrency.currencyStatus.currency) },
            )
        }.toPersistentList()

        return prevState.copy(
            bottomSheetConfig = prevState.bottomSheetConfig.copy(
                content = if (swapNetworks.isNotEmpty()) {
                    SwapChooseTokenNetworkContentUM.Content(
                        swapNetworks = swapNetworks,
                        messageContent = getErrorMessage(
                            tokenName = tokenName,
                            onDismiss = onDismiss,
                        ),
                    )
                } else {
                    SwapChooseTokenNetworkContentUM.Error(
                        messageContent = getErrorMessage(
                            tokenName = tokenName,
                            onDismiss = onDismiss,
                        ),
                    )
                },
            ),
        )
    }
}