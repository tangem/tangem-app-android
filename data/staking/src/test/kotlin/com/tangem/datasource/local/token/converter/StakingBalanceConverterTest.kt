package com.tangem.datasource.local.token.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.PendingActionConstraints
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.grow.datasource.stakekit.models.request.Address
import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO
import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO.BalanceTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.TokenDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import kotlinx.datetime.Instant
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class StakingBalanceConverterTest {

    private val tonTokenDTO = TokenDTO(
        name = "Toncoin",
        network = NetworkTypeDTO.TON,
        symbol = "TON",
        decimals = 9,
        address = null,
        coinGeckoId = "the-open-network",
        logoURI = null,
        isPoints = null,
    )
    private val tonToken = YieldToken(
        name = "Toncoin",
        network = NetworkType.TON,
        symbol = "TON",
        decimals = 9,
        address = null,
        coinGeckoId = "the-open-network",
        logoURI = null,
        isPoints = null,
    )
    private val stakingId = StakingID(integrationId = INTEGRATION_ID, address = "0x1")

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Envelope {

        @Test
        fun `GIVEN null integration id WHEN convert THEN null`() {
            // Arrange
            val wrapper = wrapper(balanceDTO(), integrationId = null)

            // Act
            val actual = StakingBalanceConverter(StatusSource.ACTUAL).convert(wrapper)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN empty balances WHEN convert THEN Empty balance with source`() {
            // Act
            val actual = StakingBalanceConverter(StatusSource.ACTUAL).convert(wrapper())

            // Assert
            assertThat(actual).isEqualTo(StakingBalance.Empty(stakingId, StatusSource.ACTUAL))
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN isCached flag WHEN convert THEN source follows the flag`(model: SourceModel) {
            // Act
            val actual = StakingBalanceConverter(isCached = model.isCached).convert(wrapper(balanceDTO()))

            // Assert
            assertThat(actual!!.source).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN one balance WHEN convert THEN StakeKit data with mapped item`() {
            // Act
            val actual = StakingBalanceConverter(StatusSource.ACTUAL).convert(wrapper(balanceDTO()))

            // Assert
            val expected = StakingBalance.Data.StakeKit(
                stakingId = stakingId,
                source = StatusSource.ACTUAL,
                balance = YieldBalanceItem(items = listOf(expectedItem()), integrationId = INTEGRATION_ID),
            )
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            SourceModel(isCached = true, expected = StatusSource.CACHE),
            SourceModel(isCached = false, expected = StatusSource.ACTUAL),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Items {

        @Test
        fun `GIVEN mixed balance types WHEN convert THEN sorted by type then amount`() {
            // Arrange
            val wrapper = wrapper(
                balanceDTO(type = BalanceTypeDTO.REWARDS, amount = BigDecimal("1")),
                balanceDTO(type = BalanceTypeDTO.STAKED, amount = BigDecimal("5")),
                balanceDTO(type = BalanceTypeDTO.STAKED, amount = BigDecimal("2")),
                balanceDTO(type = BalanceTypeDTO.AVAILABLE, amount = BigDecimal("3")),
                balanceDTO(type = BalanceTypeDTO.UNKNOWN, amount = BigDecimal("0.1")),
            )

            // Act
            val actual = convertItems(wrapper)

            // Assert
            assertThat(actual.map { it.type to it.amount }).containsExactly(
                BalanceType.AVAILABLE to BigDecimal("3"),
                BalanceType.STAKED to BigDecimal("2"),
                BalanceType.STAKED to BigDecimal("5"),
                BalanceType.REWARDS to BigDecimal("1"),
                BalanceType.UNKNOWN to BigDecimal("0.1"),
            ).inOrder()
        }

        @Test
        fun `GIVEN pending actions WHEN convert THEN sorted by passthrough`() {
            // Arrange
            val wrapper = wrapper(
                balanceDTO(
                    pendingActions = listOf(
                        BalanceDTO.PendingAction(StakingActionTypeDTO.CLAIM_REWARDS, "b", null),
                        BalanceDTO.PendingAction(StakingActionTypeDTO.WITHDRAW, "a", null),
                    ),
                ),
            )

            // Act
            val actual = convertItems(wrapper).single()

            // Assert
            assertThat(actual.pendingActions.map { it.passthrough }).containsExactly("a", "b").inOrder()
        }

        @Test
        fun `GIVEN null constraints WHEN convert THEN empty constraints`() {
            // Act
            val actual = convertItems(wrapper(balanceDTO(constraints = null))).single()

            // Assert
            assertThat(actual.pendingActionsConstraints).isEmpty()
        }

        @Test
        fun `GIVEN constraints WHEN convert THEN constraints mapped`() {
            // Arrange
            val constraints = listOf(
                BalanceDTO.PendingActionConstraints(
                    type = StakingActionTypeDTO.UNSTAKE,
                    amountArg = BalanceDTO.PendingAction.PendingActionArgs.Amount(
                        required = true,
                        minimum = BigDecimal("0.5"),
                        maximum = BigDecimal("3"),
                    ),
                ),
            )

            // Act
            val actual = convertItems(wrapper(balanceDTO(constraints = constraints))).single()

            // Assert
            val expected = PendingActionConstraints(
                StakingActionType.UNSTAKE,
                PendingAction.PendingActionArgs.Amount(true, BigDecimal("0.5"), BigDecimal("3")),
            )
            assertThat(actual.pendingActionsConstraints).containsExactly(expected)
        }

        @Test
        fun `GIVEN utc date WHEN convert THEN instant preserved`() {
            // Arrange
            val date = DateTime(EPOCH_MILLIS, DateTimeZone.UTC)

            // Act
            val actual = convertItems(wrapper(balanceDTO(date = date))).single()

            // Assert
            assertThat(actual.date).isEqualTo(Instant.fromEpochMilliseconds(EPOCH_MILLIS))
        }

        @Test
        fun `GIVEN date with offset WHEN convert THEN same instant`() {
            // Arrange
            val date = DateTime(EPOCH_MILLIS, DateTimeZone.forOffsetHours(3))

            // Act
            val actual = convertItems(wrapper(balanceDTO(date = date))).single()

            // Assert
            assertThat(actual.date).isEqualTo(Instant.fromEpochMilliseconds(EPOCH_MILLIS))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Tron {

        @Test
        fun `GIVEN tron validatorAddresses only WHEN convert THEN first address used`() {
            // Arrange
            val balance = balanceDTO(validatorAddress = null, validatorAddresses = listOf("T1", "T2"))

            // Act
            val actual = convertItems(wrapper(balance)).single()

            // Assert
            assertThat(actual.validatorAddress).isEqualTo("T1")
        }

        @Test
        fun `GIVEN both validator fields WHEN convert THEN validatorAddress wins`() {
            // Arrange
            val balance = balanceDTO(validatorAddress = "V", validatorAddresses = listOf("T1"))

            // Act
            val actual = convertItems(wrapper(balance)).single()

            // Assert
            assertThat(actual.validatorAddress).isEqualTo("V")
        }

        @Test
        fun `GIVEN no validator fields WHEN convert THEN validator address null`() {
            // Arrange
            val balance = balanceDTO(validatorAddress = null, validatorAddresses = null)

            // Act
            val actual = convertItems(wrapper(balance)).single()

            // Assert
            assertThat(actual.validatorAddress).isNull()
        }

        @Test
        fun `GIVEN empty validatorAddresses WHEN convert THEN validator address null`() {
            // Arrange
            val balance = balanceDTO(validatorAddress = null, validatorAddresses = emptyList())

            // Act
            val actual = convertItems(wrapper(balance)).single()

            // Assert
            assertThat(actual.validatorAddress).isNull()
        }
    }

    private fun convertItems(wrapper: YieldBalanceWrapperDTO): List<BalanceItem> {
        val actual = StakingBalanceConverter(StatusSource.ACTUAL).convert(wrapper)
        return (checkNotNull(actual) as StakingBalance.Data.StakeKit).balance.items
    }

    private fun expectedItem() = BalanceItem(
        groupId = "group-1",
        token = tonToken,
        type = BalanceType.STAKED,
        amount = BigDecimal.ONE,
        rawCurrencyId = "the-open-network",
        validatorAddress = "validator-1",
        date = null,
        pendingActions = emptyList(),
        pendingActionsConstraints = emptyList(),
        isPending = false,
    )

    @Suppress("LongParameterList")
    private fun balanceDTO(
        type: BalanceTypeDTO = BalanceTypeDTO.STAKED,
        amount: BigDecimal = BigDecimal.ONE,
        validatorAddress: String? = "validator-1",
        validatorAddresses: List<String>? = null,
        date: DateTime? = null,
        pendingActions: List<BalanceDTO.PendingAction> = emptyList(),
        constraints: List<BalanceDTO.PendingActionConstraints>? = null,
        groupId: String = "group-1",
    ) = BalanceDTO(
        groupId = groupId,
        type = type,
        amount = amount,
        date = date,
        pricePerShare = BigDecimal.ONE,
        pendingActions = pendingActions,
        pendingActionConstraints = constraints,
        tokenDTO = tonTokenDTO,
        validatorAddress = validatorAddress,
        validatorAddresses = validatorAddresses,
        providerId = null,
    )

    private fun wrapper(vararg balances: BalanceDTO, integrationId: String? = INTEGRATION_ID) = YieldBalanceWrapperDTO(
        addresses = Address(address = "0x1"),
        balances = balances.toList(),
        integrationId = integrationId,
    )

    internal data class SourceModel(val isCached: Boolean, val expected: StatusSource)

    private companion object {
        const val INTEGRATION_ID = MockStaking.TON_ID
        const val EPOCH_MILLIS = 1_700_000_000_000L
    }
}