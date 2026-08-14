package com.tangem.features.foryou.impl.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.foryou.model.ForYouPeriod
import com.tangem.features.foryou.model.PriceChangeIntervalToForYouPeriodConverter
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PriceChangeIntervalToForYouPeriodConverterTest {

    private val converter = PriceChangeIntervalToForYouPeriodConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = converter.convert(model.interval)

        // Assert — anything longer than a month is capped at Month
        assertThat(actual).isEqualTo(model.expected)
    }

    data class ConvertModel(val interval: PriceChangeInterval, val expected: ForYouPeriod)

    private fun provideTestModels() = listOf(
        ConvertModel(PriceChangeInterval.H24, ForYouPeriod.Day),
        ConvertModel(PriceChangeInterval.WEEK, ForYouPeriod.Week),
        ConvertModel(PriceChangeInterval.MONTH, ForYouPeriod.Month),
        ConvertModel(PriceChangeInterval.MONTH3, ForYouPeriod.Month),
        ConvertModel(PriceChangeInterval.MONTH6, ForYouPeriod.Month),
        ConvertModel(PriceChangeInterval.YEAR, ForYouPeriod.Month),
        ConvertModel(PriceChangeInterval.ALL_TIME, ForYouPeriod.Month),
    )
}