package com.tangem.domain.card.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionProcessTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a source or none WHEN ButtonImportExistingWallet built THEN the source is sent only when given`(
        model: ImportModel,
    ) {
        // Act
        val actual = IntroductionProcess.ButtonImportExistingWallet(source = model.source)

        // Assert
        assertThat(actual.id).isEqualTo("[Introduction Process] Button - Import Existing Wallet")
        assertThat(actual.params).isEqualTo(model.expectedParams)
    }

    @Test
    fun `GIVEN the add wallet sheet WHEN SheetDismissed built THEN the event matches the spec`() {
        // Act
        val actual = IntroductionProcess.SheetDismissed(
            sheet = IntroductionProcess.SheetDismissed.Sheet.AddWallet,
            reason = IntroductionProcess.SheetDismissed.DismissReason.CloseButton,
        )

        // Assert
        assertThat(actual.id).isEqualTo("[Introduction Process] Sheet Dismissed")
        assertThat(actual.params).isEqualTo(mapOf("Sheet" to "Add Wallet", "Dismiss Reason" to "Close Button"))
    }

    private fun provideTestModels() = listOf(
        ImportModel(source = null, expectedParams = mapOf("Product Type" to "Mobile Wallet")),
        ImportModel(
            source = AnalyticsParam.ScreensSources.AddNewWallet,
            expectedParams = mapOf("Product Type" to "Mobile Wallet", "Source" to "Add New Wallet"),
        ),
    )

    data class ImportModel(val source: AnalyticsParam.ScreensSources?, val expectedParams: Map<String, String>)
}