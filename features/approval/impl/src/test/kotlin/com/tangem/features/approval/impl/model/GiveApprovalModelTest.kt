package com.tangem.features.approval.impl.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetAllowanceInfoUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GiveApprovalModelTest {

    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase = mockk(relaxed = true)
    private val getAllowanceInfoUseCase: GetAllowanceInfoUseCase = mockk(relaxed = true)
    private val sendTransactionUseCase: SendTransactionUseCase = mockk(relaxed = true)
    private val getFeeUseCase: GetFeeUseCase = mockk(relaxed = true)
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase = mockk(relaxed = true)
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase = mockk(relaxed = true)
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")
    private val userWallet: UserWallet.Hot = mockk(relaxed = true)

    private val cryptoCurrencyStatus: CryptoCurrencyStatus = mockk {
        every { currency } returns mockk<CryptoCurrency.Token>(relaxed = true)
    }

    private val params = GiveApprovalComponent.Params(
        userWalletId = userWalletId,
        cryptoCurrencyStatus = cryptoCurrencyStatus,
        feeCryptoCurrencyStatus = cryptoCurrencyStatus,
        amount = "10",
        spenderAddress = "0xSpender",
        amountFooter = TextReference.EMPTY,
        feeFooter = TextReference.EMPTY,
        callback = mockk(relaxed = true),
    )

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { getUserWalletUseCase.invoke(userWalletId) } returns userWallet.right()
    }

    private fun createModel(): GiveApprovalModel = GiveApprovalModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(params),
        createApprovalTransactionUseCase = createApprovalTransactionUseCase,
        getAllowanceInfoUseCase = getAllowanceInfoUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        getFeeUseCase = getFeeUseCase,
        getFeeForGaslessUseCase = getFeeForGaslessUseCase,
        getFeeForTokenUseCase = getFeeForTokenUseCase,
        createAndSendGaslessTransactionUseCase = createAndSendGaslessTransactionUseCase,
        uiMessageSender = uiMessageSender,
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
}