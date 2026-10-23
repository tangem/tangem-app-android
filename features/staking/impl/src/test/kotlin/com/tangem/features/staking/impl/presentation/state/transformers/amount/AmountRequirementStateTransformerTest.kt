package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.google.common.truth.Truth.assertThat
import androidx.annotation.StringRes
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels
import java.math.BigDecimal

internal class AmountRequirementStateTransformerTest {

    private val boundary = EnterAmountBoundary(amount = BigDecimal("100"), fiatAmount = null, fiatRate = null)
    private val cosmosCoin = coin(Blockchain.Cosmos)
    private val solanaCoin = coin(Blockchain.Solana)
    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
    private val exit = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)

    @Test
    fun `GIVEN enter maximum WHEN amount above it THEN max amount error with integration maximum`() {
        // Arrange
        val transformer = cosmosTransformer(
            integration = cosmosIntegration(
                enter = MockStaking.enterArgs(minimum = BigDecimal("0.01"), maximum = BigDecimal("0.15")),
            ),
            actionType = enter,
        )

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.2"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error).isEqualTo(
            requirementError(R.string.staking_max_amount_requirement_error, BigDecimal("0.15"), cosmosCoin),
        )
    }

    @Test
    fun `GIVEN enter minimum WHEN amount below it THEN min amount error with integration minimum`() {
        // Arrange
        val transformer = cosmosTransformer(
            integration = cosmosIntegration(enter = MockStaking.enterArgs(minimum = BigDecimal("0.1"), maximum = null)),
            actionType = enter,
        )

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.05"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error).isEqualTo(
            requirementError(R.string.staking_amount_requirement_error, BigDecimal("0.1"), cosmosCoin),
        )
    }

    @Test
    fun `GIVEN exit minimum WHEN amount below it THEN unstake min error with exit minimum`() {
        // Arrange
        val transformer = cosmosTransformer(
            integration = cosmosIntegration(exit = MockStaking.enterArgs(minimum = BigDecimal("0.1"), maximum = null)),
            actionType = exit,
        )

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.05"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error).isEqualTo(
            requirementError(R.string.staking_unstake_amount_requirement_error, BigDecimal("0.1"), cosmosCoin),
        )
    }

    @Test
    fun `GIVEN enter without maximum WHEN amount above balance THEN max amount error with balance`() {
        // Arrange
        val transformer = cosmosTransformer(
            integration = cosmosIntegration(
                enter = MockStaking.enterArgs(minimum = BigDecimal("0.01"), maximum = null),
            ),
            actionType = enter,
            maxAmount = BigDecimal("0.5"),
        )

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.6"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error).isEqualTo(
            requirementError(R.string.staking_max_amount_requirement_error, BigDecimal("0.5"), cosmosCoin),
        )
    }

    @Test
    fun `GIVEN exit WHEN amount above staked balance THEN max amount error with staked balance`() {
        // Arrange
        val transformer = cosmosTransformer(
            integration = cosmosIntegration(exit = MockStaking.enterArgs(minimum = BigDecimal("0.01"), maximum = null)),
            actionType = exit,
            maxAmount = BigDecimal("0.5"),
        )

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.6"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error).isEqualTo(
            requirementError(R.string.staking_max_amount_requirement_error, BigDecimal("0.5"), cosmosCoin),
        )
    }

    @Test
    fun `GIVEN solana WHEN full unstake THEN no error`() {
        // Arrange
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        // Act
        val actual = transformer.transform(amountData(BigDecimal("5"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN solana stake below minimum WHEN full unstake THEN no error`() {
        // Arrange
        val small = BigDecimal("0.098090754")
        val transformer = solanaTransformer(staked = small, exitMin = BigDecimal("1"))

        // Act
        val actual = transformer.transform(amountData(small)) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN solana WHEN partial unstake below minimum THEN unstake min error and button disabled`() {
        // Arrange
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.5"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
        assertThat(actual.amountTextField.error).isEqualTo(
            requirementError(R.string.staking_unstake_amount_requirement_error, BigDecimal("1"), solanaCoin),
        )
    }

    @Test
    fun `GIVEN solana WHEN remainder below minimum THEN low staked balance error and button disabled`() {
        // Arrange
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        // Act
        val actual = transformer.transform(amountData(BigDecimal("4.5"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
        assertThat(actual.amountTextField.error)
            .isEqualTo(resourceReference(R.string.staking_notification_low_staked_balance_text))
    }

    @Test
    fun `GIVEN solana WHEN both minimums violated THEN unstake min error wins`() {
        // Arrange
        val transformer = solanaTransformer(staked = BigDecimal("1.5"), exitMin = BigDecimal("1"))

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.7"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error).isEqualTo(
            requirementError(R.string.staking_unstake_amount_requirement_error, BigDecimal("1"), solanaCoin),
        )
    }

    @Test
    fun `GIVEN solana WHEN both parts above minimum THEN no error`() {
        // Arrange
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        // Act
        val actual = transformer.transform(amountData(BigDecimal("1.5"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN solana WHEN amount equals minimum THEN no error`() {
        // Arrange
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        // Act
        val actual = transformer.transform(amountData(BigDecimal("1"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN solana WHEN remainder equals minimum THEN no error`() {
        // Arrange
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        // Act
        val actual = transformer.transform(amountData(BigDecimal("4"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
    }

    @Test
    fun `GIVEN solana zero exit minimum WHEN partial unstake THEN unstake min error with enter minimum`() {
        // Arrange
        val transformer = solanaTransformer(
            staked = BigDecimal("5"),
            exitMin = BigDecimal.ZERO,
            enterMin = BigDecimal("0.8"),
        )

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.5"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error).isEqualTo(
            requirementError(R.string.staking_unstake_amount_requirement_error, BigDecimal("0.8"), solanaCoin),
        )
    }

    @Test
    fun `GIVEN solana without minimums WHEN partial unstake THEN no error`() {
        // Arrange
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = null, enterMin = null)

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.5"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN non solana exit without exit minimum WHEN partial unstake THEN no error`() {
        // Arrange
        val transformer = cosmosTransformer(
            integration = cosmosIntegration(exit = MockStaking.enterArgs(minimum = null, maximum = null)),
            actionType = exit,
            maxAmount = BigDecimal("5"),
        )

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.5"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
    }

    @Test
    fun `GIVEN amount value null WHEN transform THEN no error and no warning`() {
        // Arrange
        val transformer = cosmosTransformer(integration = cosmosIntegration(), actionType = enter)

        // Act
        val actual = transformer.transform(amountData(cryptoAmount = null)) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
        assertThat(actual.amountTextField.isWarning).isFalse()
        assertThat(actual.amountTextField.error).isEqualTo(TextReference.EMPTY)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN non solana integration without amount requirement WHEN transform THEN no requirement error`(
        model: NoRequirementModel,
    ) {
        // Arrange
        val transformer = cosmosTransformer(
            integration = cosmosIntegration(enter = model.enter, exit = model.exit),
            actionType = model.actionType,
        )

        // Act
        val actual = transformer.transform(amountData(BigDecimal("0.05"))) as AmountState.Data

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
        assertThat(actual.amountTextField.error).isEqualTo(TextReference.EMPTY)
    }

    data class NoRequirementModel(
        val actionType: StakingActionCommonType,
        val enter: Yield.Args.Enter,
        val exit: Yield.Args.Enter?,
    )

    companion object {
        private val argsWithoutAmount = Yield.Args.Enter(
            addresses = Yield.Args.Enter.Addresses(address = AddressArgument(isRequired = true)),
            args = emptyMap(),
        )

        @JvmStatic
        fun provideTestModels() = listOf(
            NoRequirementModel(
                actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
                enter = argsWithoutAmount,
                exit = argsWithoutAmount,
            ),
            NoRequirementModel(
                actionType = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false),
                enter = argsWithoutAmount,
                exit = null,
            ),
            NoRequirementModel(
                actionType = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false),
                enter = argsWithoutAmount,
                exit = argsWithoutAmount,
            ),
        )
    }

    private fun cosmosIntegration(
        enter: Yield.Args.Enter = MockStaking.enterArgs(),
        exit: Yield.Args.Enter? = MockStaking.enterArgs(),
    ): StakingIntegration = stakeKitIntegration(
        id = StakingIntegrationID.StakeKit.Coin.Cosmos,
        yield = MockStaking.yield(
            id = MockStaking.COSMOS_ID,
            token = MockStaking.atomToken,
            enter = enter,
            exit = exit,
        ),
    )

    private fun cosmosTransformer(
        integration: StakingIntegration,
        actionType: StakingActionCommonType,
        maxAmount: BigDecimal = BigDecimal("100"),
    ) = AmountRequirementStateTransformer(
        cryptoCurrencyStatus = currencyStatus(cosmosCoin),
        maxAmount = EnterAmountBoundary(amount = maxAmount, fiatAmount = null, fiatRate = null),
        integration = integration,
        actionType = actionType,
    )

    private fun solanaTransformer(staked: BigDecimal, exitMin: BigDecimal?, enterMin: BigDecimal? = null) =
        AmountRequirementStateTransformer(
            cryptoCurrencyStatus = currencyStatus(solanaCoin),
            maxAmount = EnterAmountBoundary(amount = staked, fiatAmount = null, fiatRate = null),
            integration = stakeKitIntegration(
                yield = MockStaking.yield(
                    enter = MockStaking.enterArgs(minimum = enterMin),
                    exit = MockStaking.enterArgs(minimum = exitMin),
                ),
            ),
            actionType = exit,
        )

    private fun requirementError(@StringRes id: Int, boundary: BigDecimal, currency: CryptoCurrency): TextReference =
        resourceReference(id, wrappedList(boundary.format { crypto(currency) }))

    @Nested
    inner class Tron {

        private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)

        @Test
        fun `GIVEN tron enter fractional amount WHEN transform THEN integer warning without error`() {
            // Act
            val actual = tron(enter).transform(amountData(BigDecimal("1.5"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isWarning).isTrue()
            assertThat(actual.amountTextField.isError).isFalse()
            assertThat(actual.amountTextField.error)
                .isEqualTo(resourceReference(R.string.staking_amount_tron_integer_error, wrappedList("1")))
            assertThat(actual.isPrimaryButtonEnabled).isTrue()
        }

        @Test
        fun `GIVEN tron exit fractional amount WHEN transform THEN unstaking integer warning`() {
            // Act
            val actual = tron(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false))
                .transform(amountData(BigDecimal("1.5"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isWarning).isTrue()
            assertThat(actual.amountTextField.error)
                .isEqualTo(resourceReference(R.string.staking_amount_tron_integer_error_unstaking, wrappedList("1")))
        }

        @Test
        fun `GIVEN tron pending fractional amount WHEN transform THEN no warning`() {
            // Act
            val actual = tron(StakingActionCommonType.Pending.Other)
                .transform(amountData(BigDecimal("1.5"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isWarning).isFalse()
            assertThat(actual.amountTextField.isError).isFalse()
            assertThat(actual.amountTextField.error).isEqualTo(TextReference.EMPTY)
        }

        @Test
        fun `GIVEN tron integer amount WHEN transform THEN no warning`() {
            // Act
            val actual = tron(enter).transform(amountData(BigDecimal("2"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isWarning).isFalse()
        }

        // Pins current behaviour: an amount below the minimum is both an error and an integer warning.
        @Test
        fun `GIVEN tron fractional amount below minimum WHEN transform THEN requirement error shown with warning flag`() {
            // Act
            val actual = tron(enter).transform(amountData(BigDecimal("0.5"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isError).isTrue()
            assertThat(actual.amountTextField.isWarning).isTrue()
            assertThat(actual.amountTextField.error).isEqualTo(
                requirementError(R.string.staking_amount_requirement_error, BigDecimal.ONE, coin(Blockchain.Tron)),
            )
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN solana fractional amount WHEN transform THEN no tron warning`() {
            // Arrange
            val transformer = AmountRequirementStateTransformer(
                cryptoCurrencyStatus = currencyStatus(),
                maxAmount = boundary,
                integration = stakeKitIntegration(),
                actionType = enter,
            )

            // Act
            val actual = transformer.transform(amountData(BigDecimal("1.5"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isWarning).isFalse()
        }

        @Test
        fun `GIVEN tron fractional amount with existing field error WHEN transform THEN existing error kept`() {
            // Arrange
            val input = amountData(BigDecimal("1.5"), isError = true, error = stringReference("x"))

            // Act
            val actual = tron(enter).transform(input) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.error).isEqualTo(stringReference("x"))
            assertThat(actual.amountTextField.isError).isTrue()
        }

        @Test
        fun `GIVEN solana fractional amount WHEN transform THEN no integer error text`() {
            // Arrange
            val transformer = AmountRequirementStateTransformer(
                cryptoCurrencyStatus = currencyStatus(),
                maxAmount = boundary,
                integration = stakeKitIntegration(),
                actionType = enter,
            )

            // Act
            val actual = transformer.transform(amountData(BigDecimal("1.5"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.error).isEqualTo(TextReference.EMPTY)
        }

        private fun tron(actionType: StakingActionCommonType) = AmountRequirementStateTransformer(
            cryptoCurrencyStatus = currencyStatus(coin(Blockchain.Tron)),
            maxAmount = boundary,
            integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Tron),
            actionType = actionType,
        )
    }

    @Nested
    inner class Common {

        private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)

        @Test
        fun `GIVEN existing field error WHEN transform THEN existing error kept`() {
            // Arrange
            val input = amountData(BigDecimal("1"), isError = true, error = stringReference("x"))

            // Act
            val actual = solana(enter).transform(input) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.error).isEqualTo(stringReference("x"))
            assertThat(actual.amountTextField.isError).isTrue()
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN pending action WHEN transform THEN no requirement error`() {
            // Act
            val actual = solana(StakingActionCommonType.Pending.Rewards)
                .transform(amountData(BigDecimal("0.0001"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isError).isFalse()
        }

        @Test
        fun `GIVEN requirement not required WHEN transform THEN no error`() {
            // Arrange
            val notRequired = Yield.Args.Enter(
                addresses = Yield.Args.Enter.Addresses(address = AddressArgument(isRequired = true)),
                args = mapOf(
                    Yield.Args.ArgType.AMOUNT to MockStaking.amountArgument(minimum = BigDecimal.TEN, required = false),
                ),
            )
            val integration = stakeKitIntegration(yield = MockStaking.yield(enter = notRequired))

            // Act
            val actual = solana(enter, integration).transform(amountData(BigDecimal("1"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isError).isFalse()
        }

        @Test
        fun `GIVEN non positive enter maximum WHEN amount above balance THEN balance used as maximum`() {
            // Arrange
            val integration = stakeKitIntegration(
                yield = MockStaking.yield(enter = MockStaking.enterArgs(maximum = BigDecimal.ZERO)),
            )
            val transformer = solana(
                actionType = enter,
                integration = integration,
                maxAmount = EnterAmountBoundary(amount = BigDecimal("5"), fiatAmount = null, fiatRate = null),
            )

            // Act
            val actual = transformer.transform(amountData(BigDecimal("6"))) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isError).isTrue()
            assertThat(actual.amountTextField.error).isEqualTo(
                requirementError(R.string.staking_max_amount_requirement_error, BigDecimal("5"), solanaCoin),
            )
        }

        @Test
        fun `GIVEN fiat mode with zero fiat WHEN transform THEN primary disabled`() {
            // Arrange
            val input = amountData(BigDecimal.ONE, isFiatValue = true, fiatAmount = BigDecimal.ZERO)

            // Act
            val actual = solana(enter).transform(input) as AmountState.Data

            // Assert
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
            assertThat(actual.amountTextField.isError).isFalse()
        }

        @Test
        fun `GIVEN zero crypto amount WHEN transform THEN no requirement error and primary disabled`() {
            // Act
            val actual = solana(enter).transform(amountData(BigDecimal.ZERO)) as AmountState.Data

            // Assert
            assertThat(actual.amountTextField.isError).isFalse()
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN empty amount state WHEN transform THEN same state`() {
            // Act
            val actual = solana(enter).transform(AmountState.Empty)

            // Assert
            assertThat(actual).isSameInstanceAs(AmountState.Empty)
        }

        private fun solana(
            actionType: StakingActionCommonType,
            integration: StakingIntegration = stakeKitIntegration(),
            maxAmount: EnterAmountBoundary = boundary,
        ) = AmountRequirementStateTransformer(
            cryptoCurrencyStatus = currencyStatus(),
            maxAmount = maxAmount,
            integration = integration,
            actionType = actionType,
        )
    }
}