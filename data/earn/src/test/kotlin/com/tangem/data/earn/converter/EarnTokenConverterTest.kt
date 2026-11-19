package com.tangem.data.earn.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.earn.createEarnResponse
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnType
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnTokenConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = EarnTokenConverter.convert(createEarnResponse(type = model.apiType))

        // Assert
        assertThat(actual).isEqualTo(model.expectedType?.let(::earnToken))
    }

    private fun provideTestModels() = listOf(
        ConvertModel(apiType = "staking", expectedType = EarnType.STAKING),
        ConvertModel(apiType = "yield", expectedType = EarnType.YIELD),
        ConvertModel(apiType = "vault", expectedType = EarnType.VAULT),
        ConvertModel(apiType = "VAULT", expectedType = EarnType.VAULT),
        ConvertModel(apiType = "lending", expectedType = null),
        ConvertModel(apiType = "", expectedType = null),
    )

    internal data class ConvertModel(val apiType: String, val expectedType: EarnType?) {
        override fun toString(): String {
            val outcome = expectedType?.let { "type is $it" } ?: "the row is dropped"
            return "GIVEN api type '$apiType' WHEN converted THEN $outcome"
        }
    }

    private fun earnToken(type: EarnType) = EarnToken(
        apy = "0.031",
        networkId = "ethereum",
        rewardType = EarnRewardType.APY,
        type = type,
        tokenId = "ethereum",
        tokenSymbol = "ETH",
        tokenName = "Ethereum",
        tokenAddress = null,
        decimalCount = 18,
    )
}