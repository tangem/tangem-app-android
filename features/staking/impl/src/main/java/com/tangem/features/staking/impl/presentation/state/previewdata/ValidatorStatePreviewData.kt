package com.tangem.features.staking.impl.presentation.state.previewdata

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.Yield.Validator.ValidatorStatus
import com.tangem.features.staking.impl.presentation.state.StakingStates
import java.math.BigDecimal

internal object ValidatorStatePreviewData {

    private val validatorList = listOf(
        Yield.Validator(
            address = "0xa6e768fef2d1af36c0cfdb276422e7881a83e951",
            status = ValidatorStatus.ACTIVE,
            name = "Luganodes",
            image = "https://assets.stakek.it/validators/luganodes.png",
            apr = BigDecimal("0.054823398040640445"),
            commission = 0.1,
            stakedBalance = "355544384.45009977",
            website = "https://luganodes.com/",
            votingPower = 0.09778360195377911,
            preferred = true,
            isStrategicPartner = false,
        ),
        Yield.Validator(
            address = "0x35b1ca0f398905cf752e6fe122b51c88022fca32",
            status = ValidatorStatus.ACTIVE,
            name = "InfStones",
            image = "https://assets.stakek.it/validators/infstones.png",
            apr = BigDecimal("0.057786472172836965"),
            commission = 0.05,
            stakedBalance = "12495684.05643019",
            website = "https://infstones.com/",
            votingPower = 0.0034366257754399774,
            preferred = true,
            isStrategicPartner = true,
        ),
        Yield.Validator(
            address = "0xd14a87025109013b0a2354a775cb335f926af65a",
            status = ValidatorStatus.ACTIVE,
            name = "Kiln",
            image = "https://assets.stakek.it/validators/kiln.png",
            apr = BigDecimal("0.057786472172836965"),
            commission = 0.05,
            stakedBalance = "85400369.96393165",
            website = "https://infstones.com/",
            votingPower = 0.023487238579718264,
            preferred = true,
            isStrategicPartner = false,
        ),
    )

    val validatorState = StakingStates.ValidatorState.Data(
        availableValidators = validatorList,
        chosenValidator = validatorList.first(),
        isPrimaryButtonEnabled = true,
        activeValidator = null,
        isClickable = true,
        isVisibleOnConfirmation = true,
    )
}