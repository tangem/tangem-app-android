package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountFeeTableRow
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.moneyaccount.repository.MoneyAccountRepository
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.AccountFeeGroupType
import com.tangem.domain.pay.usecase.GetAccountFeesUseCase
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetMoneyAccountFeeTableUseCaseTest {

    private val repository: MoneyAccountRepository = mockk()
    private val getAccountFeesUseCase: GetAccountFeesUseCase = mockk()

    private val useCase = GetMoneyAccountFeeTableUseCase(
        moneyAccountRepository = repository,
        getAccountFeesUseCase = getAccountFeesUseCase,
    )

    private val types = setOf(AccountFeeGroupType.ONRAMP, AccountFeeGroupType.OFFRAMP)

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, getAccountFeesUseCase)
    }

    @Test
    fun `GIVEN fees of both groups WHEN invoke THEN joined by payment method id`() = runTest {
        // Arrange
        val ach = paymentMethod(id = "ACH_ID", name = "ACH")
        val receive = fee(paymentMethodId = ach.id, group = AccountFeeGroupType.ONRAMP, amount = BigDecimal("1.00"))
        val send = fee(paymentMethodId = ach.id, group = AccountFeeGroupType.OFFRAMP, amount = BigDecimal("1.50"))
        coEvery { repository.getPaymentMethods() } returns listOf(ach).right()
        coEvery { getAccountFeesUseCase(userWalletId, types) } returns listOf(receive, send).right()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        val expected = listOf(MoneyAccountFeeTableRow(paymentMethod = ach, receiveFee = receive, sendFee = send))
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN fee of a foreign payment method WHEN invoke THEN row columns stay empty`() = runTest {
        // Arrange
        val ach = paymentMethod(id = "ACH_ID", name = "ACH")
        val fedwireFee = fee(paymentMethodId = "FEDWIRE_ID")
        coEvery { repository.getPaymentMethods() } returns listOf(ach).right()
        coEvery { getAccountFeesUseCase(userWalletId, types) } returns listOf(fedwireFee).right()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        val expected = listOf(MoneyAccountFeeTableRow(paymentMethod = ach, receiveFee = null, sendFee = null))
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN payment methods unavailable WHEN invoke THEN error`() = runTest {
        // Arrange
        coEvery { repository.getPaymentMethods() } returns VisaApiError.ServerUnavailable.left()
        coEvery { getAccountFeesUseCase(userWalletId, types) } returns listOf(fee()).right()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ServerUnavailable.left())
    }

    @Test
    fun `GIVEN payment methods pending WHEN invoke THEN fees requested without waiting for them`() = runTest {
        // Arrange
        val ach = paymentMethod()
        val feesRequested = CompletableDeferred<Unit>()
        coEvery { repository.getPaymentMethods() } coAnswers {
            feesRequested.await()
            listOf(ach).right()
        }
        coEvery { getAccountFeesUseCase(userWalletId, types) } coAnswers {
            feesRequested.complete(Unit)
            emptyList<AccountFee>().right()
        }

        // Act
        val actual = useCase(userWalletId)

        // Assert
        val expected = listOf(MoneyAccountFeeTableRow(paymentMethod = ach, receiveFee = null, sendFee = null))
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN fees unavailable WHEN invoke THEN error`() = runTest {
        // Arrange
        coEvery { repository.getPaymentMethods() } returns listOf(paymentMethod()).right()
        coEvery { getAccountFeesUseCase(userWalletId, types) } returns VisaApiError.ServerUnavailable.left()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ServerUnavailable.left())
    }
}

internal fun paymentMethod(
    id: String = "ACH_ID",
    name: String = "ACH",
    countryCode: String = "US",
    iconUrl: String = "https://tangem.com/flags/us.png",
) = MoneyAccountPaymentMethod(id = id, name = name, countryCode = countryCode, iconUrl = iconUrl)

internal fun fee(
    type: String = "ACH_ONRAMP",
    name: String = "ACH",
    paymentMethodId: String? = "ACH_ID",
    group: AccountFeeGroupType = AccountFeeGroupType.ONRAMP,
    amount: BigDecimal = BigDecimal.ONE,
    currency: String = "USD",
) = AccountFee(
    type = type,
    name = name,
    paymentMethodId = paymentMethodId,
    group = group,
    amount = amount,
    currency = currency,
)