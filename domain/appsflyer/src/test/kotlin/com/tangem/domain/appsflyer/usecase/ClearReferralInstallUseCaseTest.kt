package com.tangem.domain.appsflyer.usecase

import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ClearReferralInstallUseCaseTest {

    private val appsFlyerRepository: AppsFlyerRepository = mockk(relaxUnitFun = true)

    private val useCase = ClearReferralInstallUseCase(appsFlyerRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(appsFlyerRepository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: ClearModel) = runTest {
        // Arrange
        coEvery { appsFlyerRepository.getNavigationDeeplink() } returns model.stored

        // Act
        useCase()

        // Assert
        coVerify(exactly = if (model.shouldClear) 1 else 0) { appsFlyerRepository.clearNavigationDeeplink() }
    }

    private fun provideTestModels(): List<ClearModel> = listOf(
        ClearModel(stored = "referral", shouldClear = true),
        ClearModel(stored = "tpay_mobileonboard", shouldClear = false),
        ClearModel(stored = "tpay_for_oxford", shouldClear = false),
        ClearModel(stored = "some_other_deep_link_value", shouldClear = false),
        ClearModel(stored = null, shouldClear = false),
    )

    internal data class ClearModel(val stored: String?, val shouldClear: Boolean)
}