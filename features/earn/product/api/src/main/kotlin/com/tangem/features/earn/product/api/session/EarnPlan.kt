package com.tangem.features.earn.product.api.session

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.product.api.product.EarnRow
import java.math.BigDecimal
import kotlin.time.Duration

interface EarnPlan {

    val fee: Fee

    val expectedAmount: BigDecimal?

    val rows: List<EarnRow>

    val withdrawMode: WithdrawMode?

    val quoteTtl: Duration?

    val notice: TextReference?

    val isEstimate: Boolean
}

sealed interface WithdrawMode {

    data object Instant : WithdrawMode

    data class Queued(val expectedBy: kotlinx.datetime.Instant?) : WithdrawMode
}

interface VerifiedPlan {

    val plan: EarnPlan
}

sealed interface EarnVerification {

    data class Verified(val plan: VerifiedPlan) : EarnVerification

    data class Changed(val newPlan: EarnPlan) : EarnVerification

    data class Rejected(val error: EarnSessionError) : EarnVerification
}