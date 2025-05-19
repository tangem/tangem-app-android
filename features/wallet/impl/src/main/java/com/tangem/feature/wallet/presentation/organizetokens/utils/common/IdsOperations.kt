package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.model.CryptoCurrency

internal fun getTokenItemId(currencyId: CryptoCurrency.ID): String = currencyId.value

internal fun getGroupHeaderId(network: Network): Int = network.hashCode()