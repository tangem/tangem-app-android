package com.tangem.domain.walletmanager.model

import java.math.BigDecimal

/**
 * Represents wallet blockchain rent
 * @param rent Amount that will be charged in overtime if the blockchain does not have an amount greater than
 * the [exemptionAmount]
 * @param exemptionAmount Amount that should be on the blockchain balance not to pay rent
 */
data class RentData(val rent: BigDecimal, val exemptionAmount: BigDecimal)