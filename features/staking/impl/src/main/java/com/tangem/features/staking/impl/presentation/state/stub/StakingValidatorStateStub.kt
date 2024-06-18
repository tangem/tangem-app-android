package com.tangem.features.staking.impl.presentation.state.stub

import com.tangem.domain.staking.model.Yield
import com.tangem.features.staking.impl.presentation.state.StakingStates

internal object StakingValidatorStateStub {
    private val validator = Yield.Validator(
        address = "address",
        status = "status",
        name = "Binance",
        image = null,
        website = null,
        apr = "0.0354".toBigDecimal(),
        commission = null,
        stakedBalance = null,
        votingPower = null,
        preferred = false,
    )

    val state = StakingStates.ValidatorState.Data(
        isPrimaryButtonEnabled = false,
        validators = listOf(
            validator,
            validator.copy(
                address = "TRONLink",
                name = "TRONLink",
                apr = "0.0506".toBigDecimal(),
            ),
            validator.copy(
                address = "1inch",
                name = "1inch",
                apr = "0.0306".toBigDecimal(),
            ),
        ),
        selectedValidator = validator,
    )
}