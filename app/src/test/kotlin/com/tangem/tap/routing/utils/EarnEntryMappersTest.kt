package com.tangem.tap.routing.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.EarnEntryStart
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class EarnEntryMappersTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToEarnEntrySource {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN route source WHEN toEarnEntrySource THEN api source matches`(model: SourceModel) {
            // Act
            val actual = model.source.toEarnEntrySource()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SourceModel(AppRoute.EarnEntry.Source.TokenPage, EarnEntrySource.TokenPage),
            SourceModel(AppRoute.EarnEntry.Source.Markets, EarnEntrySource.Markets),
            SourceModel(AppRoute.EarnEntry.Source.Deeplink, EarnEntrySource.Deeplink),
            SourceModel(AppRoute.EarnEntry.Source.EarnBanner, EarnEntrySource.EarnBanner),
            SourceModel(AppRoute.EarnEntry.Source.YieldSheet, EarnEntrySource.YieldSheet),
            SourceModel(AppRoute.EarnEntry.Source.ForYou, EarnEntrySource.ForYou),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToEarnEntryStart {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN route start WHEN toEarnEntryStart THEN api start matches`(model: StartModel) {
            // Act
            val actual = model.start.toEarnEntryStart()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            StartModel(AppRoute.EarnEntry.Start.Auto, EarnEntryStart.Auto),
            StartModel(AppRoute.EarnEntry.Start.Picker(preselected = null), EarnEntryStart.Picker(preselected = null)),
            StartModel(
                start = AppRoute.EarnEntry.Start.Picker(preselected = LENDING),
                expected = EarnEntryStart.Picker(preselected = LENDING),
            ),
            StartModel(
                start = AppRoute.EarnEntry.Start.Info(productId = LENDING),
                expected = EarnEntryStart.Info(productId = LENDING),
            ),
        )
    }

    internal data class SourceModel(val source: AppRoute.EarnEntry.Source, val expected: EarnEntrySource) {
        override fun toString(): String = source.toString()
    }

    internal data class StartModel(val start: AppRoute.EarnEntry.Start, val expected: EarnEntryStart) {
        override fun toString(): String = start.toString()
    }

    private companion object {
        val LENDING = EarnProductId("lending")
    }
}