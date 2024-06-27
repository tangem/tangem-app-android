package com.tangem.features.staking.impl.presentation.state.previewdata

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType.Coin
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.Yield
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object ConfirmStakingStatePreviewData {

    private val validatorList = listOf(
        Yield.Validator(
            address = "0xa6e768fef2d1af36c0cfdb276422e7881a83e951",
            status = "active",
            name = "Luganodes",
            image = "https://assets.stakek.it/validators/luganodes.png",
            apr = BigDecimal("0.054823398040640445"),
            commission = 0.1,
            stakedBalance = "355544384.45009977",
            website = "https://luganodes.com/",
            votingPower = 0.09778360195377911,
            preferred = true,
        ),
        Yield.Validator(
            address = "0x35b1ca0f398905cf752e6fe122b51c88022fca32",
            status = "active",
            name = "InfStones",
            image = "https://assets.stakek.it/validators/infstones.png",
            apr = BigDecimal("0.057786472172836965"),
            commission = 0.05,
            stakedBalance = "12495684.05643019",
            website = "https://infstones.com/",
            votingPower = 0.0034366257754399774,
            preferred = true,
        ),
        Yield.Validator(
            address = "0xd14a87025109013b0a2354a775cb335f926af65a",
            status = "active",
            name = "Kiln",
            image = "https://assets.stakek.it/validators/kiln.png",
            apr = BigDecimal("0.057786472172836965"),
            commission = 0.05,
            stakedBalance = "85400369.96393165",
            website = "https://infstones.com/",
            votingPower = 0.023487238579718264,
            preferred = true,
        ),
    )

    private val fee = Fee.Common(
        amount = Amount(
            currencySymbol = "MATIC",
            value = BigDecimal(0.159806),
            decimals = 18,
            type = Coin,
        ),
    )

    val confirmStakingState = StakingStates.ConfirmStakingState.Data(
        isPrimaryButtonEnabled = true,
        feeState = FeeState.Content(
            fee = fee,
            rate = BigDecimal.ONE,
            appCurrency = AppCurrency.Default,
            isFeeApproximate = false,
            isFeeConvertibleToFiat = true,
        ),
        validatorState = ValidatorState.Content(
            chosenValidator = validatorList[0],
            availableValidators = validatorList,
        ),
        footerText = "You stake \$715.11 and will be receiving ~\$35 monthly",
        notifications = persistentListOf(
            StakingNotification.Warning.EarnRewards(
                currencyName = "Solana",
                days = 2,
            ),
        ),
        isStaking = false,
        isSuccess = false,
    )
}
