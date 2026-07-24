package com.tangem.datasource.local.promotion

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPromotionsSupplierTest {

    private val tangemApi: TangemTechApi = mockk()

    private fun newSupplier() = DefaultPromotionsSupplier(
        tangemApi = tangemApi,
        store = RuntimeSharedStore(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("abcdef012345")
    private val response = PromotionsResponse(promotions = emptyList())
    private val response2 = PromotionsResponse(
        promotions = listOf(
            PromotionsResponse.PromotionDto(name = "dummy", all = null),
        ),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemApi)
    }

    @Test
    fun `GIVEN empty cache WHEN getPromotions THEN fetches and returns response`() = runTest {
        // Arrange
        coEvery { tangemApi.getPromotions(any(), any()) } returns ApiResponse.Success(response)
        val supplier = newSupplier()

        // Act
        val result = supplier.getPromotions(userWalletId)

        // Assert
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) { tangemApi.getPromotions(userWalletId.stringValue, any()) }
    }

    @Test
    fun `GIVEN cached value and no refresh WHEN getPromotions THEN returns cache without api`() = runTest {
        // Arrange
        coEvery { tangemApi.getPromotions(any(), any()) } returns ApiResponse.Success(response)
        val supplier = newSupplier()
        supplier.getPromotions(userWalletId)
        clearMocks(tangemApi)

        // Act
        val result = supplier.getPromotions(userWalletId, forceRefresh = false)

        // Assert
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 0) { tangemApi.getPromotions(any(), any()) }
    }

    @Test
    fun `GIVEN cached value WHEN getPromotions forceRefresh THEN hits api again and rebinds value`() = runTest {
        // Arrange
        coEvery { tangemApi.getPromotions(any(), any()) } returnsMany listOf(
            ApiResponse.Success(response),
            ApiResponse.Success(response2),
        )
        val supplier = newSupplier()
        supplier.getPromotions(userWalletId)

        // Act
        val result = supplier.getPromotions(userWalletId, forceRefresh = true)

        // Assert
        assertThat(result).isEqualTo(response2)
        coVerify(exactly = 2) { tangemApi.getPromotions(any(), any()) }
    }

    @Test
    fun `GIVEN fetch fails and cache present WHEN getPromotions forceRefresh THEN rethrows`() = runTest {
        // Arrange
        coEvery { tangemApi.getPromotions(any(), any()) } returns ApiResponse.Success(response)
        val supplier = newSupplier()
        supplier.getPromotions(userWalletId)
        coEvery { tangemApi.getPromotions(any(), any()) } throws IOException("boom")

        // Act
        val error = runCatching { supplier.getPromotions(userWalletId, forceRefresh = true) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IOException::class.java)
    }

    @Test
    fun `GIVEN fetch fails and empty cache WHEN getPromotions THEN rethrows`() = runTest {
        // Arrange
        coEvery { tangemApi.getPromotions(any(), any()) } throws IOException("boom")
        val supplier = newSupplier()

        // Act
        val error = runCatching { supplier.getPromotions(userWalletId) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IOException::class.java)
    }
}