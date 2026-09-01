package com.tangem.features.feed.earn.model.filters.state

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.features.feed.earn.ui.state.EarnFilterTypeUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnFilterTypeConverterTest {

    private val converter = EarnFilterTypeConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: TypeModel) {
        // Act
        val actual = converter.convert(model.filter)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TypeModel(
            name = "GIVEN all types WHEN converted THEN the sheet preselects the all option",
            filter = EarnFilterType.ALL,
            expected = EarnFilterTypeUM.All,
        ),
        TypeModel(
            name = "GIVEN staking WHEN converted THEN the sheet preselects staking",
            filter = EarnFilterType.STAKING,
            expected = EarnFilterTypeUM.Staking,
        ),
        TypeModel(
            name = "GIVEN yield WHEN converted THEN the sheet preselects yield mode",
            filter = EarnFilterType.YIELD,
            expected = EarnFilterTypeUM.YieldMode,
        ),
    )

    internal data class TypeModel(
        val name: String,
        val filter: EarnFilterType,
        val expected: EarnFilterTypeUM,
    ) {
        override fun toString(): String = name
    }
}