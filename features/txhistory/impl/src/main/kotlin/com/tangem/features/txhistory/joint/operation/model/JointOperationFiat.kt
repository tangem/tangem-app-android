package com.tangem.features.txhistory.joint.operation.model

import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

/** What the sheet needs to price the operation: the currency's [rate] in the selected [appCurrency], if quoted */
internal data class JointOperationFiat(val rate: BigDecimal?, val appCurrency: AppCurrency)