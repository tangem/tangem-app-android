package com.tangem.domain.pay.flow

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

interface PaymentAccountStatusFetcher : FlowFetcher<PaymentAccountStatusFetcher.Params> {
    data class Params(val userWalletId: UserWalletId)
}