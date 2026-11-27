package com.tangem.domain.tangempay.utils

import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.model.CustomerSummary

val CustomerSummary.isKycAdditionalInfoRequested: Boolean
    get() = customerStatus == CustomerSummary.CustomerStatus.ACTIVE &&
        kycStatus == CustomerSummary.KycStatus.IN_PROGRESS

val CustomerInfo.isKycAdditionalInfoRequested: Boolean
    get() = state == CustomerInfo.State.ACTIVE && kycStatus == KycStatus.PENDING

val PaymentAccountStatusValue.Loaded.isKycAdditionalInfoActionRequired: Boolean
    get() = isKycAdditionalInfoRequired && !isPaymentAccountIssuing