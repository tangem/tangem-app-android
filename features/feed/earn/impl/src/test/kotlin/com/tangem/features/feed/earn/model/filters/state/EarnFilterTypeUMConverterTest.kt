package com.tangem.features.feed.earn.model.filters.state

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.features.feed.earn.ui.state.EarnFilterTypeUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnFilterTypeUMConverterTest {

    private val converter = EarnFilterTypeUMConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: TypeUMModel) {
        // Act
        val actual = converter.convert(model.option)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN every option WHEN converted back and forth THEN the filter is unchanged`() {
        // Act
        val actual = EarnFilterType.entries.map { converter.convert(EarnFilterTypeConverter().convert(it)) }

        // Assert
        assertThat(actual).containsExactlyElementsIn(EarnFilterType.entries).inOrder()
    }

    private fun provideTestModels() = listOf(
        TypeUMModel(
            name = "GIVEN the all option WHEN converted THEN the filter stops narrowing by type",
            option = EarnFilterTypeUM.All,
            expected = EarnFilterType.ALL,
        ),
        TypeUMModel(
            name = "GIVEN the staking option WHEN converted THEN the filter narrows to staking",
            option = EarnFilterTypeUM.Staking,
            expected = EarnFilterType.STAKING,
        ),
        TypeUMModel(
            name = "GIVEN the yield mode option WHEN converted THEN the filter narrows to yield",
            option = EarnFilterTypeUM.YieldMode,
            expected = EarnFilterType.YIELD,
        ),
    )

    internal data class TypeUMModel(
        val name: String,
        val option: EarnFilterTypeUM,
        val expected: EarnFilterType,
    ) {
        override fun toString(): String = name
    }
}