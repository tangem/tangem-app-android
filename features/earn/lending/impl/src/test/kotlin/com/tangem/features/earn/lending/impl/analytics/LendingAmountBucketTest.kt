package com.tangem.features.earn.lending.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LendingAmountBucketTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN usd amount WHEN of THEN falls into its range`(model: BucketModel) {
        // Act
        val actual = LendingAmountBucket.of(BigDecimal(model.usd))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class BucketModel(val usd: String, val expected: String)

    private fun provideTestModels() = listOf(
        BucketModel(usd = "0", expected = "<50"),
        BucketModel(usd = "49.99", expected = "<50"),
        BucketModel(usd = "50", expected = "50-250"),
        BucketModel(usd = "249.99", expected = "50-250"),
        BucketModel(usd = "250", expected = "250-1K"),
        BucketModel(usd = "1000", expected = "1K-5K"),
        BucketModel(usd = "5000", expected = "5K-25K"),
        BucketModel(usd = "25000", expected = "25K-100K"),
        BucketModel(usd = "99999.99", expected = "25K-100K"),
        BucketModel(usd = "100000", expected = ">100K"),
    )
}