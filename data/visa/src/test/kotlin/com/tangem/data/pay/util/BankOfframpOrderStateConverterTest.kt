package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.toHexString
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.spend.datasource.pay.models.response.OrderResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.web3j.crypto.StructuredDataEncoder

/**
 * The state this converter reports decides whether the card is asked to sign an order, so every response shape is
 * pinned here rather than only through the repository that happens to call it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankOfframpOrderStateConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an order response WHEN convert THEN the matching state returned`(model: ConvertModel) {
        // Act
        val actual = BankOfframpOrderStateConverter.convert(model.response)

        // Assert
        when (val expected = model.expected) {
            is Expected.Exactly -> assertThat(actual).isEqualTo(expected.state)
            Expected.Unsignable -> assertThat(actual).isInstanceOf(BankOfframpOrderState.Failed::class.java)
        }
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        ConvertModel(
            response = result(step = AWAITING_SIGNATURE, withdrawData = withdrawData(hash = CONSISTENT_HASH)),
            expected = Expected.Exactly(BankOfframpOrderState.AwaitingSignature(payloadHash = CONSISTENT_HASH)),
        ),
        ConvertModel(
            response = result(step = AWAITING_SIGNATURE, withdrawData = null),
            expected = Expected.Unsignable,
        ),
        ConvertModel(
            response = result(
                step = AWAITING_SIGNATURE,
                withdrawData = withdrawData(hash = CONSISTENT_HASH, structuredData = null),
            ),
            expected = Expected.Unsignable,
        ),
        ConvertModel(
            response = result(step = AWAITING_SIGNATURE, withdrawData = withdrawData(hash = "0xdeadbeef")),
            expected = Expected.Unsignable,
        ),
        ConvertModel(
            response = result(status = OrderResponse.Result.Status.CANCELED),
            expected = Expected.Exactly(BankOfframpOrderState.Canceled),
        ),
        ConvertModel(
            response = result(status = OrderResponse.Result.Status.COMPLETED),
            expected = Expected.Exactly(BankOfframpOrderState.Completed),
        ),
        ConvertModel(
            response = result(
                status = OrderResponse.Result.Status.COMPLETED,
                step = AWAITING_SIGNATURE,
                withdrawData = withdrawData(hash = CONSISTENT_HASH),
            ),
            expected = Expected.Exactly(BankOfframpOrderState.Completed),
        ),
        ConvertModel(
            response = result(status = OrderResponse.Result.Status.NEW, step = "AWAITING_DEPOSIT"),
            expected = Expected.Exactly(BankOfframpOrderState.Processing(OrderStep.AWAITING_DEPOSIT)),
        ),
        ConvertModel(
            response = result(status = OrderResponse.Result.Status.PROCESSING, step = "AWAITING_DEPOSIT"),
            expected = Expected.Exactly(BankOfframpOrderState.Processing(OrderStep.AWAITING_DEPOSIT)),
        ),
        ConvertModel(
            response = result(status = OrderResponse.Result.Status.UNKNOWN, step = "AWAITING_DEPOSIT"),
            expected = Expected.Exactly(BankOfframpOrderState.Processing(OrderStep.AWAITING_DEPOSIT)),
        ),
        ConvertModel(
            response = result(status = OrderResponse.Result.Status.PROCESSING, step = "SOMETHING_NEW"),
            expected = Expected.Exactly(BankOfframpOrderState.Processing(OrderStep.UNKNOWN)),
        ),
        ConvertModel(
            response = result(status = OrderResponse.Result.Status.PROCESSING, step = null),
            expected = Expected.Exactly(BankOfframpOrderState.Processing(OrderStep.UNKNOWN)),
        ),
    )

    private fun withdrawData(hash: String, structuredData: String? = STRUCTURED_DATA) =
        OrderResponse.Result.Data.WithdrawData(
            hash = hash,
            salt = "0x01",
            senderAddress = "0xSENDER",
            structuredData = structuredData,
        )

    private fun result(
        status: OrderResponse.Result.Status = OrderResponse.Result.Status.PROCESSING,
        step: String? = null,
        withdrawData: OrderResponse.Result.Data.WithdrawData? = null,
    ) = OrderResponse.Result(
        id = "order-1",
        customerId = "customer-1",
        type = "BANK_OFFRAMP_RAIN",
        status = status,
        step = step,
        data = OrderResponse.Result.Data(
            type = "BANK_OFFRAMP_RAIN",
            specificationName = null,
            customerWalletAddress = null,
            embossName = null,
            productInstanceId = null,
            paymentAccountId = null,
            targetTariffPlanId = null,
            transactionHash = null,
            bankAccountId = null,
            withdrawData = withdrawData,
        ),
        stepChangeCode = null,
        createdAt = null,
        updatedAt = null,
    )

    /**
     * [Unsignable] stands for every response the app refuses to sign: those map to a state carrying the cause as a
     * [Throwable], which has no structural equality, so such rows are matched by type instead.
     */
    internal sealed interface Expected {
        data class Exactly(val state: BankOfframpOrderState) : Expected
        data object Unsignable : Expected
    }

    internal data class ConvertModel(val response: OrderResponse.Result, val expected: Expected)

    private companion object {
        const val AWAITING_SIGNATURE = "AWAITING_SIGNATURE"

        val STRUCTURED_DATA = """
            {
              "types": {
                "EIP712Domain": [
                  {"name": "name", "type": "string"},
                  {"name": "chainId", "type": "uint256"}
                ],
                "Withdraw": [
                  {"name": "sender", "type": "address"},
                  {"name": "amount", "type": "uint256"}
                ]
              },
              "primaryType": "Withdraw",
              "domain": {"name": "Rain", "chainId": 137},
              "message": {
                "sender": "0x059dbd5f24a000d28f83268db1c85c9c3ab6a123",
                "amount": "10000000"
              }
            }
        """.trimIndent()

        val CONSISTENT_HASH = "0x" + StructuredDataEncoder(STRUCTURED_DATA).hashStructuredData().toHexString()
    }
}