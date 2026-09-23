package com.tangem.data.staking.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import org.junit.jupiter.api.Test

internal class YieldConverterTest {

    @Test
    fun `GIVEN validator without name WHEN convert THEN name falls back to a placeholder`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(address = POOL_ADDRESS, name = null))

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.single().name).isEqualTo("No name")
    }

    @Test
    fun `GIVEN validator with name WHEN convert THEN name is kept`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(address = POOL_ADDRESS, name = "Tangem"))

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.single().name).isEqualTo("Tangem")
    }

    @Test
    fun `GIVEN nameless validator WHEN convertListIgnoreErrors THEN yield is not dropped`() {
        // Arrange
        val yieldDTO = createYieldDTO(
            createValidatorDTO(address = "pool1", name = null),
            createValidatorDTO(address = "pool2", name = "Named pool"),
        )

        // Act
        val actual = YieldConverter.convertListIgnoreErrors(listOf(yieldDTO))

        // Assert
        assertThat(actual).hasSize(1)
        assertThat(actual.single().validators.map { it.name }).containsExactly("No name", "Named pool")
    }

    @Test
    fun `GIVEN partner name WHEN convert THEN validator is a strategic partner`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(address = POOL_ADDRESS, name = "Meria"))

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.single().isStrategicPartner).isTrue()
    }

    @Test
    fun `GIVEN validator without preferred flag WHEN convert THEN validator is not preferred`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(preferred = null))

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.single().preferred).isFalse()
    }

    @Test
    fun `GIVEN validator without address WHEN convert THEN error is thrown`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(address = null, name = "Named pool"))

        // Act
        val actual = runCatching { YieldConverter.convert(yieldDTO) }.exceptionOrNull()

        // Assert
        assertThat(actual).hasMessageThat().isEqualTo("address must not be null")
    }

    private fun createYieldDTO(vararg validators: YieldDTO.ValidatorDTO): YieldDTO {
        return MockYieldDTOFactory.create().copy(validators = validators.toList())
    }

    private fun createValidatorDTO(
        address: String? = POOL_ADDRESS,
        name: String? = null,
        status: ValidatorStatusDTO? = ValidatorStatusDTO.ACTIVE,
        preferred: Boolean? = false,
    ): YieldDTO.ValidatorDTO {
        return YieldDTO.ValidatorDTO(
            address = address,
            status = status,
            name = name,
            image = null,
            website = null,
            apr = null,
            commission = null,
            stakedBalance = null,
            votingPower = null,
            preferred = preferred,
        )
    }

    private companion object {
        const val POOL_ADDRESS = "19521342b6c4cefb13f091a075a221a3e765435a0646902dd5379f3a"
    }
}