package com.tangem.domain.tangempay.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType

data class KycAdditionalInfoAccount(val userWalletId: UserWalletId, val accountType: AccountType)