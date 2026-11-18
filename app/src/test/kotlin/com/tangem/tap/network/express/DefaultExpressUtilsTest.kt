package com.tangem.tap.network.express

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultExpressUtilsTest {

    private val expressUtils = DefaultExpressUtils(
        appPreferencesStore = mockk(),
        storePreferencesKeys = mockk(),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN wallet WHEN getBatchId THEN returns card batch id for cold wallets only`(model: GetBatchIdModel) {
        // Act
        val actual = expressUtils.getBatchId(model.userWallet)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        GetBatchIdModel(userWallet = createColdWallet(batchId = BATCH_ID), expected = BATCH_ID),
        GetBatchIdModel(userWallet = mockk<UserWallet.Hot>(), expected = null),
        GetBatchIdModel(userWallet = null, expected = null),
    )

    private fun createColdWallet(batchId: String): UserWallet.Cold {
        val scanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(maxWalletCount = 2),
            derivedKeys = emptyMap(),
        )
        return MockUserWalletFactory.create(
            scanResponse = scanResponse.copy(card = scanResponse.card.copy(batchId = batchId)),
        )
    }

    data class GetBatchIdModel(val userWallet: UserWallet?, val expected: String?)

    private companion object {
        const val BATCH_ID = "AF990015"
    }
}