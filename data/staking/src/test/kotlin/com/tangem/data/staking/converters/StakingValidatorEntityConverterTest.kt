package com.tangem.data.staking.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import com.tangem.store.datasource.txhistory.db.entity.staking.StakingValidatorEntity
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class StakingValidatorEntityConverterTest {

    @Test
    fun `GIVEN validator without address WHEN convert THEN null`() {
        // Arrange
        val dto = createDTO(address = null)

        // Act
        val actual = StakingValidatorEntityConverter.convert(dto)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN full validator WHEN convert THEN entity stores enum name and plain apr`() {
        // Arrange
        val dto = YieldDTO.ValidatorDTO(
            address = "v1",
            status = ValidatorStatusDTO.ACTIVE,
            name = "Node",
            image = "img",
            website = "web",
            apr = BigDecimal("1E-7"),
            commission = 0.1,
            stakedBalance = "100",
            votingPower = 0.5,
            preferred = true,
        )

        // Act
        val actual = StakingValidatorEntityConverter.convert(dto)

        // Assert
        val expected = StakingValidatorEntity(
            address = "v1",
            status = "ACTIVE",
            name = "Node",
            image = "img",
            website = "web",
            apr = "0.0000001",
            commission = 0.1,
            stakedBalance = "100",
            votingPower = 0.5,
            isPreferred = true,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN validator with null optionals WHEN convert THEN entity keeps nulls`() {
        // Act
        val actual = StakingValidatorEntityConverter.convert(createDTO(address = "v1"))

        // Assert
        val expected = StakingValidatorEntity(
            address = "v1",
            status = null,
            name = null,
            image = null,
            website = null,
            apr = null,
            commission = null,
            stakedBalance = null,
            votingPower = null,
            isPreferred = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN validator dto WHEN converted to entity and back THEN matches live conversion except reward type`() {
        // Arrange
        val dto = YieldDTO.ValidatorDTO(
            address = "v1",
            status = ValidatorStatusDTO.JAILED,
            name = "Node",
            image = null,
            website = null,
            apr = BigDecimal("0.05"),
            commission = null,
            stakedBalance = null,
            votingPower = null,
            preferred = false,
        )
        val liveYield = MockYieldDTOFactory.create()
            .copy(rewardType = YieldDTO.RewardTypeDTO.APR, validators = listOf(dto))

        // Act
        val fromEntity = YieldConverter.convertFromEntity(StakingValidatorEntityConverter.convert(dto)!!)
        val live = YieldConverter.convert(liveYield).allValidators.single()

        // Assert
        assertThat(fromEntity).isEqualTo(live.copy(rewardInfo = RewardInfo(BigDecimal("0.05"), RewardType.UNKNOWN)))
    }

    private fun createDTO(address: String?) = YieldDTO.ValidatorDTO(
        address = address,
        status = null,
        name = null,
        image = null,
        website = null,
        apr = null,
        commission = null,
        stakedBalance = null,
        votingPower = null,
        preferred = null,
    )
}