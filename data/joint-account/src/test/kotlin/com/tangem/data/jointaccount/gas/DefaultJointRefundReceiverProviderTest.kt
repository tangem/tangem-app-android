package com.tangem.data.jointaccount.gas

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointRefundReceiverProviderTest {

    private val apiConfigsManager: ApiConfigsManager = mockk()

    private val provider = DefaultJointRefundReceiverProvider(apiConfigsManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(apiConfigsManager)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN tangem tech environment WHEN refundReceiver THEN returns its treasury`(model: RefundReceiverModel) =
        runTest {
            // Arrange
            every { apiConfigsManager.getEnvironmentConfig(TangemGatewayApiConfig.TANGEM_TECH_ID) } returns
                ApiEnvironmentConfig(environment = model.environment, baseUrl = "https://api.tangem.org/")

            // Act
            val actual = provider.refundReceiver()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

    private fun provideTestModels(): List<RefundReceiverModel> = ApiEnvironment.entries.map { environment ->
        val isProd = environment == ApiEnvironment.PROD || environment == ApiEnvironment.PROD_NEW
        RefundReceiverModel(
            environment = environment,
            expected = if (isProd) {
                DefaultJointRefundReceiverProvider.PROD_TREASURY
            } else {
                DefaultJointRefundReceiverProvider.TEST_TREASURY
            },
        )
    }

    data class RefundReceiverModel(val environment: ApiEnvironment, val expected: String)
}