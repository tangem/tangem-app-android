package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network

internal fun getTokenItemId(currencyId: CryptoCurrency.ID): String = currencyId.value

internal fun getGroupHeaderId(networkId: Network.ID): String = networkId.value