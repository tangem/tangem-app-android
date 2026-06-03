package com.tangem.tap.common.pushes

import arrow.core.Either
import com.tangem.common.routing.deeplink.DeeplinkConst.DERIVATION_PATH_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokenDetailsPushHandlerTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val singleAccountListFetcher: SingleAccountListFetcher = mockk()

    private val handler = TokenDetailsPushHandler(
        appCoroutineScope = mockk<AppCoroutineScope>(),
        getUserWalletUseCase = getUserWalletUseCase,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        singleAccountListSupplier = singleAccountListSupplier,
        singleAccountListFetcher = singleAccountListFetcher,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        mockkObject(TangemLogger)
        coEvery { singleAccountListFetcher.invoke(any()) } returns Either.Right(Unit)
    }

    @Test
    fun `GIVEN token absent in portfolio WHEN handle push THEN refresh accounts`() = runTest {
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(multiCurrencyWallet())
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList(currencies = emptyList())

        handler.refreshPortfolioIfTokenMissing(defaultData())

        coVerify { singleAccountListFetcher.invoke(SingleAccountListFetcher.Params(userWalletId)) }
    }

    @Test
    fun `GIVEN token present in portfolio WHEN handle push THEN do not refresh`() = runTest {
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(multiCurrencyWallet())
        coEvery {
            singleAccountListSupplier.getSyncOrNull(userWalletId)
        } returns accountList(currencies = listOf(mockCryptoCurrency()))

        handler.refreshPortfolioIfTokenMissing(defaultData())

        coVerify(exactly = 0) { singleAccountListFetcher.invoke(any()) }
    }

    @Test
    fun `GIVEN no wallet id in payload WHEN handle push THEN refresh selected wallet`() = runTest {
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(multiCurrencyWallet())
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList(currencies = emptyList())

        handler.refreshPortfolioIfTokenMissing(defaultData() - WALLET_ID_KEY)

        coVerify { singleAccountListFetcher.invoke(SingleAccountListFetcher.Params(userWalletId)) }
    }

    @Test
    fun `GIVEN no wallet id and no selected wallet WHEN handle push THEN do not refresh`() = runTest {
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Left(GetUserWalletError.UserWalletNotFound)

        handler.refreshPortfolioIfTokenMissing(defaultData() - WALLET_ID_KEY)

        coVerify(exactly = 0) { singleAccountListFetcher.invoke(any()) }
    }

    @Test
    fun `GIVEN locked wallet WHEN handle push THEN do not refresh`() = runTest {
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk { every { isLocked } returns true },
        )

        handler.refreshPortfolioIfTokenMissing(defaultData())

        coVerify(exactly = 0) { singleAccountListFetcher.invoke(any()) }
    }

    @Test
    fun `GIVEN single currency wallet WHEN handle push THEN do not refresh`() = runTest {
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isLocked } returns false
                every { isMultiCurrency } returns false
            },
        )

        handler.refreshPortfolioIfTokenMissing(defaultData())

        coVerify(exactly = 0) { singleAccountListFetcher.invoke(any()) }
    }

    @Test
    fun `GIVEN wallet not found WHEN handle push THEN do not refresh`() = runTest {
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Left(
            value = GetUserWalletError.UserWalletNotFound,
        )

        handler.refreshPortfolioIfTokenMissing(defaultData())

        coVerify(exactly = 0) { singleAccountListFetcher.invoke(any()) }
    }

    private fun defaultData() = mapOf(
        WALLET_ID_KEY to "011",
        NETWORK_ID_KEY to "123",
        TOKEN_ID_KEY to "321",
        DERIVATION_PATH_KEY to "777",
    )

    private fun multiCurrencyWallet(): UserWallet = mockk {
        every { isLocked } returns false
        every { isMultiCurrency } returns true
        every { walletId } returns userWalletId
    }

    private fun accountList(currencies: List<CryptoCurrency>): AccountList = AccountList.empty(
        userWalletId = userWalletId,
        cryptoCurrencies = currencies,
    )

    private fun mockCryptoCurrency() = mockk<CryptoCurrency> {
        every { network } returns mockk {
            every { rawId } returns "123"
            every { derivationPath } returns Network.DerivationPath.Card(value = "777")
        }
        every { id } returns CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(rawId = "321", derivationPath = "777"),
            suffix = CryptoCurrency.ID.Suffix.RawID("321"),
        )
    }
}