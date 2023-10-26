package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network

internal fun getTokenItemId(currencyId: CryptoCurrency.ID): String = currencyId.value

internal fun getGroupHeaderId(network: Network): Int = network.hashCode()