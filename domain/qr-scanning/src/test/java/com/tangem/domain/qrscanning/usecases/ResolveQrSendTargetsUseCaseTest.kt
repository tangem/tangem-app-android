package com.tangem.domain.qrscanning.usecases

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import com.tangem.domain.qrscanning.models.QrSendTarget
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveQrSendTargetsUseCaseTest {

    private val multiAccountListSupplier: MultiAccountListSupplier = mockk(relaxed = true)
    private val qrScanningEventsRepository: QrScanningEventsRepository = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val networksRepository: NetworksRepository = mockk(relaxed = true)
    private val currency: CryptoCurrency = mockk(relaxed = true)

    private val useCase = ResolveQrSendTargetsUseCase(
        multiAccountListSupplier = multiAccountListSupplier,
        qrScanningEventsRepository = qrScanningEventsRepository,
        userWalletsListRepository = userWalletsListRepository,
        networksRepository = networksRepository,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun burnAddress(model: BurnAddressModel) = runTest {
        // Arrange — a burn address is well-formed, so the classifier itself accepts it
        coEvery { multiAccountListSupplier.getSyncOrNull(Unit) } returns null
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        every { qrScanningEventsRepository.classify(model.qrCode, any()) } returns model.classified

        // Act
        val actual = useCase(model.qrCode)

        // Assert — the send screen must not open at all for a recipient nobody can spend from
        assertThat(actual).isInstanceOf(QrSendTarget.Error::class.java)
        val error = (actual as QrSendTarget.Error).error
        assertThat(error).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
        assertThat((error as ClassifiedQrContent.Error.Unrecognized).raw).isEqualTo(model.qrCode)
    }

    internal data class BurnAddressModel(val qrCode: String, val classified: ClassifiedQrContent)

    private fun provideTestModels() = listOf(
        BurnAddressModel(
            qrCode = ZERO_ADDRESS,
            classified = ClassifiedQrContent.PlainAddress(
                address = ZERO_ADDRESS,
                matchingCurrencies = listOf(currency),
            ),
        ),
        BurnAddressModel(
            qrCode = DEAD_ADDRESS,
            classified = ClassifiedQrContent.PlainAddress(
                address = DEAD_ADDRESS,
                matchingCurrencies = listOf(currency),
            ),
        ),
        // ERC-681 with the burn address as the transfer() recipient — the shape that skips the recipient screen
        BurnAddressModel(
            qrCode = "ethereum:$USDT_CONTRACT@1/transfer?address=$DEAD_ADDRESS&uint256=1000000",
            classified = ClassifiedQrContent.PaymentUri(
                address = DEAD_ADDRESS,
                amount = null,
                memo = null,
                matchingCurrencies = listOf(currency),
            ),
        ),
        // Warning wrapper must not smuggle a burn recipient through either
        BurnAddressModel(
            qrCode = "ethereum:$DEAD_ADDRESS@1?value=1000&unknown=1",
            classified = ClassifiedQrContent.PaymentUriWarning(
                paymentUri = ClassifiedQrContent.PaymentUri(
                    address = DEAD_ADDRESS,
                    amount = null,
                    memo = null,
                    matchingCurrencies = listOf(currency),
                ),
                unsupportedParams = mapOf("unknown" to "1"),
            ),
        ),
    )

    @Test
    fun `GIVEN regular address WHEN resolve THEN not rejected as unrecognized`() = runTest {
        // Arrange
        coEvery { multiAccountListSupplier.getSyncOrNull(Unit) } returns null
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        every { qrScanningEventsRepository.classify(RECIPIENT, any()) } returns ClassifiedQrContent.PlainAddress(
            address = RECIPIENT,
            matchingCurrencies = listOf(currency),
        )

        // Act
        val actual = useCase(RECIPIENT)

        // Assert — with an empty portfolio it resolves to AddressSameAsWallet, but never to an error
        assertThat(actual).isNotInstanceOf(QrSendTarget.Error::class.java)
    }

    private companion object {
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
        const val DEAD_ADDRESS = "0x000000000000000000000000000000000000dEaD"
        const val RECIPIENT = "0xfc9013965447f804042a03ae4b98130a8c300a2f"
        const val USDT_CONTRACT = "0xdac17f958d2ee523a2206206994597c13d831ec7"
    }
}