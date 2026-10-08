package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.toStakingTarget
import com.tangem.utils.toBriefAddressFormat

/**
 * Resolves the target an existing balance sits at, including jailed and inactive validators.
 *
 * StakeKit may still report a balance at a validator it no longer lists. Without a target the row would be
 * dropped, and a null target makes the action flow fall back to a preferred validator — building the action
 * for the wrong address. A non-preferred target keeps the original address and stays out of the picker.
 */
internal fun StakingIntegration.findBalanceTarget(address: String?, name: String? = null): StakingTarget? {
    if (address.isNullOrBlank()) return null
    return allTargets.firstOrNull { address.contains(it.address, ignoreCase = true) }
        ?: createUnlistedTarget(address = address, name = name)
}

private fun StakingIntegration.createUnlistedTarget(address: String, name: String?): StakingTarget? {
    if (this !is StakeKitIntegration) return null
    return Yield.Validator(
        address = address,
        status = Yield.Validator.ValidatorStatus.UNKNOWN,
        name = name ?: address.toBriefAddressFormat(),
        preferred = false,
        isStrategicPartner = false,
    ).toStakingTarget()
}