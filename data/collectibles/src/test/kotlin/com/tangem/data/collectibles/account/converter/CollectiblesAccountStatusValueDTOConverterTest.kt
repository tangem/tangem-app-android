package com.tangem.data.collectibles.account.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.entity.CollectiblesAccountStatusValueDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesAccountStatusValueDTOConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = CollectiblesAccountStatusValueDTOConverter.convert(model.status)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN a stored state WHEN converted back THEN it comes back as cached`() {
        // Act
        val actual = CollectiblesAccountStatusValueDTO.entries.map(
            CollectiblesAccountStatusValueDTOConverter::convertBack,
        )

        // Assert
        assertThat(actual).containsExactly(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE),
            CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.CACHE),
        ).inOrder()
    }

    internal data class ConvertModel(
        val status: CollectiblesAccountStatusValue,
        val expected: CollectiblesAccountStatusValueDTO?,
    )

    private fun provideTestModels() = listOf(
        ConvertModel(
            status = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
            expected = CollectiblesAccountStatusValueDTO.ONBOARDED,
        ),
        ConvertModel(
            status = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL),
            expected = CollectiblesAccountStatusValueDTO.UPDATE_REQUIRED,
        ),
        ConvertModel(status = CollectiblesAccountStatusValue.Loading, expected = null),
        ConvertModel(status = CollectiblesAccountStatusValue.NotOnboarded, expected = null),
        ConvertModel(status = CollectiblesAccountStatusValue.Error.Unavailable, expected = null),
    )
}