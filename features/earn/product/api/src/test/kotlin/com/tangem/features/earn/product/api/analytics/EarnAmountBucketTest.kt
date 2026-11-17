package com.tangem.features.earn.product.api.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnAmountBucketTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN usd amount WHEN of THEN bucket with the boundary in the upper one`(model: BucketModel) {
        // Act
        val actual = EarnAmountBucket.of(BigDecimal(model.usd))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        BucketModel(usd = "0", expected = "<50"),
        BucketModel(usd = "49.99", expected = "<50"),
        BucketModel(usd = "50", expected = "50-250"),
        BucketModel(usd = "249.99", expected = "50-250"),
        BucketModel(usd = "250", expected = "250-1K"),
        BucketModel(usd = "999.99", expected = "250-1K"),
        BucketModel(usd = "1000", expected = "1K-5K"),
        BucketModel(usd = "4999.99", expected = "1K-5K"),
        BucketModel(usd = "5000", expected = "5K-25K"),
        BucketModel(usd = "24999.99", expected = "5K-25K"),
        BucketModel(usd = "25000", expected = "25K-100K"),
        BucketModel(usd = "99999.99", expected = "25K-100K"),
        BucketModel(usd = "100000", expected = ">100K"),
        BucketModel(usd = "5000000", expected = ">100K"),
    )

    internal data class BucketModel(val usd: String, val expected: String) {
        override fun toString(): String = "$usd -> $expected"
    }
}