package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface EarnProduct {

    data class Params(val userWalletId: UserWalletId, val currency: CryptoCurrency)
}

data class EarnProvider(val title: TextReference, val icon: ImageReference)