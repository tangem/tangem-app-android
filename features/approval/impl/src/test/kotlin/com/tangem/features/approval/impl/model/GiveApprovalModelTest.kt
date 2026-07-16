package com.tangem.features.approval.impl.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetAllowanceInfoUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class GiveApprovalModelTest {

    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase = mockk(relaxed = true)
    private val getAllowanceInfoUseCase: GetAllowanceInfoUseCase = mockk(relaxed = true)
    private val sendTransactionUseCase: SendTransactionUseCase = mockk(relaxed = true)
    private val getFeeUseCase: GetFeeUseCase = mockk(relaxed = true)
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase = mockk(relaxed = true)
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase = mockk(relaxed = true)
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")
    private val userWallet: UserWallet.Hot = mockk(relaxed = true)

    private val tokenCurrency: CryptoCurrency.Token = mockk(relaxed = true) {
        every { contractAddress } returns "0xContract"
        every { network } returns mockk<Network>(relaxed = true)
    }

    private val cryptoCurrencyStatus: CryptoCurrencyStatus = mockk {
        every { currency } returns tokenCurrency
    }

    private val approvalTx: TransactionData.Uncompiled = mockk(relaxed = true)
    private val transactionFee: TransactionFee = mockk(relaxed = true)
    private val transactionFeeExtended: TransactionFeeExtended = mockk(relaxed = true) {
        every { transactionFee } returns this@GiveApprovalModelTest.transactionFee
    }

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { getUserWalletUseCase.invoke(userWalletId) } returns userWallet.right()

        coEvery {
            createApprovalTransactionUseCase(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = any(),
                contractAddress = any(),
                spenderAddress = any(),
            )
        } returns approvalTx.right()

        coEvery {
            getAllowanceInfoUseCase(
                userWalletId = any(),
                cryptoCurrency = any(),
                spenderAddress = any(),
                requiredAmount = any(),
            )
        } returns AllowanceInfo.Enough(allowance = BigDecimal.ZERO).right()

        coEvery {
            getFeeUseCase(transactionData = any(), userWallet = any(), network = any())
        } returns transactionFee.right()

        coEvery {
            getFeeForGaslessUseCase(transactionData = any(), userWallet = any(), network = any())
        } returns transactionFeeExtended.right()

        coEvery {
            getFeeForTokenUseCase(transactionData = any(), userWallet = any(), token = any())
        } returns transactionFeeExtended.right()
    }

    private fun createParams(amount: String): GiveApprovalComponent.Params = GiveApprovalComponent.Params(
        userWalletId = userWalletId,
        cryptoCurrencyStatus = cryptoCurrencyStatus,
        feeCryptoCurrencyStatus = cryptoCurrencyStatus,
        amount = amount,
        spenderAddress = "0xSpender",
        amountFooter = TextReference.EMPTY,
        feeFooter = TextReference.EMPTY,
        callback = mockk(relaxed = true),
    )

    private fun createModel(amount: String = "10"): GiveApprovalModel = GiveApprovalModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(createParams(amount)),
        createApprovalTransactionUseCase = createApprovalTransactionUseCase,
        getAllowanceInfoUseCase = getAllowanceInfoUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        getFeeUseCase = getFeeUseCase,
        getFeeForGaslessUseCase = getFeeForGaslessUseCase,
        getFeeForTokenUseCase = getFeeForTokenUseCase,
        createAndSendGaslessTransactionUseCase = createAndSendGaslessTransactionUseCase,
        urlOpener = urlOpener,
        getUserWalletUseCase = getUserWalletUseCase,
        analyticsEventHandler = analyticsEventHandler,
        feeSelectorReloadTrigger = feeSelectorReloadTrigger,
    )

    @Test
    fun `GIVEN approveType LIMITED WHEN onChangeApproveType THEN triggers fee reload and updates state`() = runTest {
        val model = createModel()

        model.onChangeApproveType(ApproveType.UNLIMITED)

        val state = model.uiState.value
        assertThat(state.approveType).isEqualTo(ApproveType.UNLIMITED)
        assertThat(state.isApproveButtonEnabled).isFalse()
        coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN same approveType WHEN onChangeApproveType THEN does not trigger fee reload`() = runTest {
        val model = createModel()

        model.onChangeApproveType(ApproveType.LIMITED)

        assertThat(model.uiState.value.approveType).isEqualTo(ApproveType.LIMITED)
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
        coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate() }
    }

    @Test
    fun `GIVEN comma decimal amount and LIMITED approveType WHEN loadFeeExtended THEN creates approval tx with parsed amount`() =
        runTest {
            val model = createModel(amount = "1,1")

            val result = model.loadFeeExtended(maybeToken = null)

            assertThat(result.isRight()).isTrue()
            coVerify {
                createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = match { it != null && it.compareTo(BigDecimal("1.1")) == 0 },
                    contractAddress = any(),
                    spenderAddress = any(),
                )
            }
        }

    @Test
    fun `GIVEN point decimal amount and LIMITED approveType WHEN loadFeeExtended THEN creates approval tx with parsed amount`() =
        runTest {
            val model = createModel(amount = "1.1")

            val result = model.loadFeeExtended(maybeToken = null)

            assertThat(result.isRight()).isTrue()
            coVerify {
                createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = match { it != null && it.compareTo(BigDecimal("1.1")) == 0 },
                    contractAddress = any(),
                    spenderAddress = any(),
                )
            }
        }

    @Test
    fun `GIVEN comma decimal amount and UNLIMITED approveType WHEN loadFeeExtended THEN creates approval tx with null amount`() =
        runTest {
            val model = createModel(amount = "1,1")
            model.onChangeApproveType(ApproveType.UNLIMITED)

            val result = model.loadFeeExtended(maybeToken = null)

            assertThat(result.isRight()).isTrue()
            coVerify {
                createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = isNull(),
                    contractAddress = any(),
                    spenderAddress = any(),
                )
            }
        }

    @Test
    fun `GIVEN unparseable amount and LIMITED approveType WHEN loadFeeExtended THEN creates approval tx with null amount`() =
        runTest {
            val model = createModel(amount = "abc")

            val result = model.loadFeeExtended(maybeToken = null)

            assertThat(result.isRight()).isTrue()
            coVerify {
                createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = isNull(),
                    contractAddress = any(),
                    spenderAddress = any(),
                )
            }
        }

    @Test
    fun `GIVEN comma decimal amount and LIMITED approveType WHEN loadFee THEN creates approval tx with parsed amount`() =
        runTest {
            val model = createModel(amount = "2,5")

            val result = model.loadFee()

            assertThat(result.isRight()).isTrue()
            coVerify {
                createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = match { it != null && it.compareTo(BigDecimal("2.5")) == 0 },
                    contractAddress = any(),
                    spenderAddress = any(),
                )
            }
        }

    @Test
    fun `GIVEN unparseable amount WHEN loadFee THEN returns DataError and does not check allowance`() = runTest {
        val model = createModel(amount = "abc")

        val result = model.loadFee()

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.DataError::class.java)
        coVerify(exactly = 0) {
            getAllowanceInfoUseCase(
                userWalletId = any(),
                cryptoCurrency = any(),
                spenderAddress = any(),
                requiredAmount = any(),
            )
        }
    }
}