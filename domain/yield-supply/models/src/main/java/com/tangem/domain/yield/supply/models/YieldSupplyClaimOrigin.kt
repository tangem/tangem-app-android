package com.tangem.domain.yield.supply.models

/**
 * What the owner was doing when a Merkl rewards claim was signed.
 *
 * It decides whether a failed claim is worth reporting on its own: a claim started from the claim sheet is an
 * action the user took and expects an answer to, while one that rides along with a withdrawal was never announced
 * to them — telling them minutes later that a transaction failed would point at the withdrawal they did make and
 * that did succeed.
 */
enum class YieldSupplyClaimOrigin {
    ClaimSheet,
    Withdrawal,
}