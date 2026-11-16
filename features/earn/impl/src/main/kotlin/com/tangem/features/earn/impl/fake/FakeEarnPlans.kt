package com.tangem.features.earn.impl.fake

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.VerifiedPlan
import com.tangem.features.earn.product.api.session.WithdrawMode
import java.math.BigDecimal
import kotlin.time.Duration

internal data class FakeDepositPlan(
    override val fee: Fee,
    override val expectedAmount: BigDecimal?,
    override val rows: List<EarnRow>,
    override val withdrawMode: WithdrawMode?,
    override val quoteTtl: Duration?,
    override val notice: TextReference?,
    override val isEstimate: Boolean,
    val actionId: String,
    val isFeeRefreshed: Boolean = false,
) : EarnPlan

internal data class FakeVerifiedPlan(override val plan: EarnPlan) : VerifiedPlan