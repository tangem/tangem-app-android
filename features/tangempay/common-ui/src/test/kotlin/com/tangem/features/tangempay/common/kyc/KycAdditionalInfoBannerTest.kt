package com.tangem.features.tangempay.common.kyc

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class KycAdditionalInfoBannerTest {

    @Test
    fun `GIVEN callback WHEN createKycAdditionalInfoBannerState THEN warning banner with provide button`() {
        // Arrange
        val onProvideInformationClick: () -> Unit = {}

        // Act
        val actual = createKycAdditionalInfoBannerState(onProvideInformationClick = onProvideInformationClick)

        // Assert
        val expected = TangemMessageBanner.State(
            title = resourceReference(R.string.tangempay_kyc_additional_info_title),
            variant = TangemMessageBanner.Variant.Warning,
            description = resourceReference(R.string.tangempay_kyc_additional_info_banner_description),
            secondaryButton = TangemMessageBanner.Button(
                text = resourceReference(R.string.tangempay_kyc_additional_info_button),
                onClick = onProvideInformationClick,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN banner state WHEN provide button clicked THEN callback invoked once`() {
        // Arrange
        val onProvideInformationClick: () -> Unit = mockk(relaxed = true)
        val state = createKycAdditionalInfoBannerState(onProvideInformationClick = onProvideInformationClick)

        // Act
        state.secondaryButton?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { onProvideInformationClick() }
    }
}