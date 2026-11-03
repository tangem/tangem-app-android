package com.tangem.features.earn.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.EarnEntryStart
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.navigation.EarnRoute
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnEntryInitialRouteTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN entry start WHEN toInitialRoute THEN picker route matches`(model: InitialRouteModel) {
        // Act
        val actual = model.start.toInitialRoute()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        InitialRouteModel(
            start = EarnEntryStart.Picker(),
            expected = EarnPickerRoute(preselected = null, shouldRedirectToActive = false),
        ),
        InitialRouteModel(
            start = EarnEntryStart.Picker(preselected = LENDING),
            expected = EarnPickerRoute(preselected = LENDING, shouldRedirectToActive = false),
        ),
        InitialRouteModel(
            start = EarnEntryStart.Info(productId = LENDING),
            expected = EarnPickerRoute(preselected = LENDING, shouldRedirectToActive = false),
        ),
        InitialRouteModel(
            start = EarnEntryStart.Auto,
            expected = EarnPickerRoute(preselected = null, shouldRedirectToActive = true),
        ),
    )

    internal data class InitialRouteModel(val start: EarnEntryStart, val expected: EarnRoute) {
        override fun toString(): String = start.toString()
    }

    private companion object {
        val LENDING = EarnProductId("lending")
    }
}