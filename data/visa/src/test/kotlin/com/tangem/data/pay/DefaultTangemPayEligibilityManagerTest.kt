package com.tangem.data.pay

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTangemPayEligibilityManagerTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsListRepository, onboardingRepository)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        coEvery { onboardingRepository.isTangemPayDeactivated(USER_WALLET_ID) } returns false
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN wallet customer state WHEN get compatible wallets excluding customers THEN expected wallets returned`(
        model: CompatibleWalletsModel,
    ) = runTest {
        // Arrange
        coEvery { onboardingRepository.hasTangemPayInWallet(USER_WALLET_ID) } returns model.hasTangemPay.right()
        every { onboardingRepository.getSavedCustomerInfo(USER_WALLET_ID) } returns
            model.savedKycStatus?.let(::customerInfo)
        val manager = DefaultTangemPayEligibilityManager(
            userWalletsListRepository = userWalletsListRepository,
            coroutineScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
            onboardingRepository = onboardingRepository,
        )

        // Act
        val actual = manager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        CompatibleWalletsModel(hasTangemPay = false, savedKycStatus = null, expected = listOf(USER_WALLET_ID)),
        CompatibleWalletsModel(hasTangemPay = true, savedKycStatus = null, expected = emptyList()),
        CompatibleWalletsModel(hasTangemPay = true, savedKycStatus = KycStatus.INIT, expected = emptyList()),
        CompatibleWalletsModel(
            hasTangemPay = true,
            savedKycStatus = KycStatus.REQUIRED,
            expected = listOf(USER_WALLET_ID),
        ),
    )

    private val userWallet: UserWallet.Hot = mockk {
        every { walletId } returns USER_WALLET_ID
        every { isLocked } returns false
    }

    private fun customerInfo(kycStatus: KycStatus): CustomerInfo = mockk {
        every { this@mockk.kycStatus } returns kycStatus
    }

    data class CompatibleWalletsModel(
        val hasTangemPay: Boolean,
        val savedKycStatus: KycStatus?,
        val expected: List<UserWalletId>,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}