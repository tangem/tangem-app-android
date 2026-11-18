package com.tangem.domain.collectibles.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectiblesRegionStatus
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCollectiblesRegionStatusUseCaseTest {

    private val repository: CollectiblesRepository = mockk()

    private val useCase = GetCollectiblesRegionStatusUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the backend verdict WHEN invoke THEN it is mapped to a status`(model: VerdictModel) = runTest {
        // Arrange
        coEvery { repository.isRegionBlocked(WALLET_ID, any()) } returns model.isBlocked

        // Act
        val actual = useCase(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN skip cache WHEN invoke THEN it reaches the repository`(skipCache: Boolean) = runTest {
        // Arrange
        coEvery { repository.isRegionBlocked(any(), any()) } returns false

        // Act
        useCase(WALLET_ID, skipCache = skipCache)

        // Assert
        coVerify(exactly = 1) { repository.isRegionBlocked(userWalletId = WALLET_ID, skipCache = skipCache) }
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN the region is unknown`() = runTest {
        // Arrange
        coEvery { repository.isRegionBlocked(any(), any()) } throws IllegalStateException("backend is down")

        // Act
        val actual = useCase(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CollectiblesRegionStatus.UNKNOWN)
    }

    private fun provideTestModels() = listOf(
        VerdictModel(isBlocked = true, expected = CollectiblesRegionStatus.RESTRICTED),
        VerdictModel(isBlocked = false, expected = CollectiblesRegionStatus.ALLOWED),
    )

    data class VerdictModel(val isBlocked: Boolean, val expected: CollectiblesRegionStatus)

    private companion object {

        val WALLET_ID = UserWalletId("aa")
    }
}