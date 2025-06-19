package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.transformers

import com.tangem.core.ui.extensions.iconResId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkContentUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.SwapChooseTokenFactory.getErrorMessage
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class SwapChooseContentStateTransformer(
    private val pairs: SwapCurrencies,
    private val tokenName: String,
    private val onNetworkClick: (CryptoCurrency) -> Unit,
    private val onDismiss: () -> Unit,
) : Transformer<SwapChooseTokenNetworkUM> {
    override fun transform(prevState: SwapChooseTokenNetworkUM): SwapChooseTokenNetworkUM {
        val swapNetworks = pairs.fromGroup.available.map {
            val network = it.currencyStatus.currency.network
            SwapChooseNetworkUM(
                title = stringReference(network.name),
                subtitle = stringReference(network.standardType.name),
                iconResId = network.iconResId,
                hasFixedRate = it.providers.any { it.rateTypes.any { it == ExpressRateType.Fixed } },
                onNetworkClick = { onNetworkClick(it.currencyStatus.currency) },
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