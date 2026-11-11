package com.tangem.tap.common.pushes

import arrow.core.Either
import com.tangem.common.routing.deeplink.DeeplinkConst.ACCOUNT_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountPushHandlerTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val singleAccountListFetcher: SingleAccountListFetcher = mockk()

    private val handler = JointAccountPushHandler(
        appCoroutineScope = mockk(),
        foregroundActivityProvider = mockk(),
        jointAccountFeatureToggles = mockk(),
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountListSupplier = singleAccountListSupplier,
        singleAccountListFetcher = singleAccountListFetcher,
    )

    private val userWalletId = UserWalletId("011")
    private val accountValue = "a".repeat(n = 64)
    private val accountId = AccountId.forJointAccount(userWalletId = userWalletId, value = accountValue).getOrNull()!!

    @BeforeEach
    fun resetMocks() {
        clearMocks(getUserWalletUseCase, singleAccountListSupplier, singleAccountListFetcher)
        coEvery { singleAccountListFetcher(any()) } returns Either.Right(Unit)
    }

    @Test
    fun `GIVEN pushed joint account in unlocked multi-currency wallet WHEN refresh THEN refreshes account list`() =
        runTest {
            // Arrange
            givenEnvironment(PushModel(name = "valid"))

            // Act
            handler.refreshWalletIfJointAccount(validData())

            // Assert
            coVerify(exactly = 1) {
                singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId = userWalletId))
            }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN unsuitable push or state WHEN refresh THEN push is ignored`(model: PushModel) = runTest {
        // Arrange
        givenEnvironment(model)

        // Act
        handler.refreshWalletIfJointAccount(model.data)

        // Assert
        coVerify(exactly = 0) { singleAccountListFetcher(any()) }
    }

    private fun givenEnvironment(model: PushModel) {
        every { getUserWalletUseCase(userWalletId) } returns
            (model.userWallet?.let { Either.Right(it) } ?: Either.Left(GetUserWalletError.UserWalletNotFound))
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns
            model.accounts?.let { accounts -> mockk<AccountList> { every { this@mockk.accounts } returns accounts } }
    }

    private fun validData(): Map<String, String> = mapOf(
        TYPE_KEY to "joint_tokens",
        WALLET_ID_KEY to userWalletId.stringValue,
        ACCOUNT_ID_KEY to accountValue,
    )

    private fun provideTestModels() = listOf(
        PushModel(name = "malformed wallet id", data = validData() + (WALLET_ID_KEY to "not-hex")),
        PushModel(name = "no wallet id", data = validData() - WALLET_ID_KEY),
        PushModel(name = "malformed account id", data = validData() + (ACCOUNT_ID_KEY to "xyz")),
        PushModel(name = "no account id", data = validData() - ACCOUNT_ID_KEY),
        PushModel(name = "wallet not found", userWallet = null),
        PushModel(name = "locked wallet", userWallet = wallet(isLocked = true)),
        PushModel(name = "single-currency wallet", userWallet = wallet(isMultiCurrency = false)),
        PushModel(name = "account list not loaded", accounts = null),
        PushModel(name = "account not found", accounts = emptyList()),
        PushModel(name = "account is not joint", accounts = listOf(personalAccount())),
    )

    private fun wallet(isLocked: Boolean = false, isMultiCurrency: Boolean = true): UserWallet = mockk<UserWallet.Cold> {
        every { this@mockk.isLocked } returns isLocked
        every { this@mockk.isMultiCurrency } returns isMultiCurrency
        every { walletId } returns userWalletId
    }

    private fun personalAccount(): Account.Personal = mockk {
        every { accountId } returns this@JointAccountPushHandlerTest.accountId
    }

    private fun jointAccount(): Account.Joint = mockk { every { accountId } returns this@JointAccountPushHandlerTest.accountId }

    inner class PushModel(
        val name: String,
        val data: Map<String, String> = validData(),
        val userWallet: UserWallet? = wallet(),
        val accounts: List<Account>? = listOf(jointAccount()),
    ) {
        override fun toString(): String = name
    }
}