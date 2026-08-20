package com.tangem.features.feed.earn.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.earn.createEarnToken
import com.tangem.features.feed.earn.impl.R
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * The badge is a styled reference, so its span-style lambda is left alone — resolving it would need a
 * Composition. The resource id and the formatted APY are what the test pins.
 */
internal class EarnTokenTextTest {

    private lateinit var defaultLocale: Locale

    @BeforeEach
    fun pinLocale() {
        // The percent format reads Locale.getDefault(), so the decimal separator would otherwise depend
        // on the machine running the test.
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN an APR reward WHEN the badge is built THEN the APR wording is used`() {
        // Act
        val actual = createEarnToken(rewardType = EarnRewardType.APR).earnValueText()

        // Assert
        assertThat((actual as TextReference.StyledRes).id).isEqualTo(R.string.staking_apr_earn_badge)
    }

    @Test
    fun `GIVEN an APY reward WHEN the badge is built THEN the APY wording is used`() {
        // Act
        val actual = createEarnToken(rewardType = EarnRewardType.APY).earnValueText()

        // Assert
        assertThat((actual as TextReference.StyledRes).id).isEqualTo(R.string.yield_module_earn_badge)
    }

    @Test
    fun `GIVEN a fractional apy WHEN the badge is built THEN it is shown as a percentage`() {
        // Act — the backend sends a fraction, the badge shows percent, so 0.055 has to read as 5.50
        val actual = createEarnToken(apy = "0.055").earnValueText() as TextReference.StyledRes

        // Assert
        assertThat(actual.formatArgs).isEqualTo(wrappedList<Any>(TextReference.Str("5.50")))
    }

    @Test
    fun `GIVEN more precision than fits WHEN the badge is built THEN the apy is rounded half up`() {
        // Act
        val actual = createEarnToken(apy = "0.05555").earnValueText() as TextReference.StyledRes

        // Assert
        assertThat(actual.formatArgs).isEqualTo(wrappedList<Any>(TextReference.Str("5.56")))
    }

    @Test
    fun `GIVEN a negative apy WHEN the badge is built THEN it is shown without its sign`() {
        // Act
        val actual = createEarnToken(apy = "-0.02").earnValueText() as TextReference.StyledRes

        // Assert
        assertThat(actual.formatArgs).isEqualTo(wrappedList<Any>(TextReference.Str("2.00")))
    }

    @Test
    fun `GIVEN an apy that is not a number WHEN the badge is built THEN building it fails`() {
        // Act
        val exception = runCatching { createEarnToken(apy = "n/a").earnValueText() }.exceptionOrNull()

        // Assert
        assertThat(exception).isInstanceOf(NumberFormatException::class.java)
    }

    @Test
    fun `GIVEN staking WHEN the title is built THEN it names staking`() {
        // Act
        val actual = EarnType.STAKING.toTitleText()

        // Assert
        assertThat(actual).isEqualTo(TextReference.Res(R.string.common_staking))
    }

    @Test
    fun `GIVEN yield WHEN the title is built THEN it names yield mode`() {
        // Act
        val actual = EarnType.YIELD.toTitleText()

        // Assert
        assertThat(actual).isEqualTo(TextReference.Res(R.string.common_yield_mode))
    }
}