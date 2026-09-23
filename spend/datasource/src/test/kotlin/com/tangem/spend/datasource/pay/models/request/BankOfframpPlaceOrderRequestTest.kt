package com.tangem.spend.datasource.pay.models.request

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.spend.datasource.pay.addTangemPayEnumFallbackAdapters
import com.tangem.spend.datasource.pay.models.response.RawJsonAdapter
import org.junit.jupiter.api.Test

/**
 * The bank off-ramp order body is a contract the backend validates key by key, so every test here asserts the
 * whole serialized shape — the keys that must be there and the ones that must not.
 */
internal class BankOfframpPlaceOrderRequestTest {

    private val moshi = Moshi.Builder()
        .addTangemPayEnumFallbackAdapters()
        .add(RawJsonAdapter())
        .build()

    private val requestAdapter = moshi.adapter(BankOfframpPlaceOrderRequest::class.java)

    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    @Test
    fun `GIVEN saved recipient WHEN serialized THEN data has exactly the mode A keys`() {
        // Arrange
        val request = createRequest(bankAccountId = BANK_ACCOUNT_ID)

        // Act
        val json = serialize(request)

        // Assert
        assertThat(json.keys).containsExactly("data", "idempotency_key")
        assertThat(json["idempotency_key"]).isEqualTo(IDEMPOTENCY_KEY)
        assertThat(json.child("data")).isEqualTo(
            parse(
                """{"type":"BANK_OFFRAMP_RAIN","chain_id":137,"amount_in_cents":10000,""" +
                    """"token_contract_address":"$TOKEN_CONTRACT_ADDRESS","bank_account_id":"$BANK_ACCOUNT_ID"}""",
            ),
        )
    }

    @Test
    fun `GIVEN new recipient WHEN serialized THEN data has exactly the mode B keys`() {
        // Arrange
        val request = createRequest(bankAccount = createBankAccount())

        // Act
        val data = serialize(request).child("data")

        // Assert
        assertThat(data.keys)
            .containsExactly("type", "chain_id", "amount_in_cents", "token_contract_address", "bank_account")
        assertThat(data["type"]).isEqualTo("BANK_OFFRAMP_RAIN")
    }

    @Test
    fun `GIVEN new recipient WHEN serialized THEN the bank account matches the spec example`() {
        // Arrange
        val request = createRequest(bankAccount = createBankAccount())

        // Act
        val bankAccount = serialize(request).child("data").child("bank_account")

        // Assert
        assertThat(bankAccount).isEqualTo(parse(EXPECTED_BANK_ACCOUNT))
    }

    @Test
    fun `GIVEN business beneficiary WHEN serialized THEN business_name sent and personal names omitted`() {
        // Arrange
        val beneficiary = BankOfframpPlaceOrderRequest.Beneficiary(type = "BUSINESS", businessName = "Tangem AG")
        val request = createRequest(bankAccount = createBankAccount(beneficiary = beneficiary))

        // Act
        val serialized = serialize(request).child("data").child("bank_account").child("details").child("beneficiary")

        // Assert
        assertThat(serialized).isEqualTo(parse("""{"type":"BUSINESS","business_name":"Tangem AG"}"""))
    }

    @Test
    fun `GIVEN null optional fields WHEN serialized THEN keys omitted`() {
        // Arrange
        val request = createRequest(bankAccount = createBankAccount(address = createAddress(line2 = null)))

        // Act
        val bankAccount = serialize(request).child("data").child("bank_account")

        // Assert
        assertThat(bankAccount.keys).containsExactly("details")
        assertThat(bankAccount.child("details").child("beneficiary_address").keys)
            .containsExactly("line1", "city", "region", "postal_code", "country_code")
    }

    private fun createRequest(
        bankAccountId: String? = null,
        bankAccount: BankOfframpPlaceOrderRequest.BankAccount? = null,
    ) = BankOfframpPlaceOrderRequest(
        data = BankOfframpPlaceOrderRequest.Data(
            chainId = 137,
            amountInCents = 10_000,
            tokenContractAddress = TOKEN_CONTRACT_ADDRESS,
            bankAccountId = bankAccountId,
            bankAccount = bankAccount,
        ),
        idempotencyKey = IDEMPOTENCY_KEY,
    )

    private fun createBankAccount(
        beneficiary: BankOfframpPlaceOrderRequest.Beneficiary = BankOfframpPlaceOrderRequest.Beneficiary(
            type = "INDIVIDUAL",
            firstName = "John",
            lastName = "Doe",
        ),
        address: BankOfframpPlaceOrderRequest.Address = createAddress(),
    ) = BankOfframpPlaceOrderRequest.BankAccount(
        details = BankOfframpPlaceOrderRequest.Details(
            currency = "USD",
            rail = "WIRE",
            isThirdParty = false,
            beneficiary = beneficiary,
            beneficiaryAddress = address,
            bankName = "Chase Bank",
            bankAddress = address,
            accountNumber = "1234567890",
            routingNumber = "021000021",
            bankAccountType = "SAVINGS",
        ),
    )

    private fun createAddress(line2: String? = "Apt 4") = BankOfframpPlaceOrderRequest.Address(
        line1 = "123 Main St",
        line2 = line2,
        city = "New York",
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )

    private fun serialize(request: BankOfframpPlaceOrderRequest): Map<String, Any?> = parse(requestAdapter.toJson(request))

    private fun parse(json: String): Map<String, Any?> = requireNotNull(mapAdapter.fromJson(json))

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.child(key: String): Map<String, Any?> = this[key] as Map<String, Any?>

    private companion object {
        const val IDEMPOTENCY_KEY = "a3f1c0de"
        const val BANK_ACCOUNT_ID = "7b0f7d70"
        const val TOKEN_CONTRACT_ADDRESS = "0xUSDC"

        val EXPECTED_BANK_ACCOUNT = """{"details":{"currency":"USD","rail":"WIRE","third_party":false,""" +
            """"beneficiary":{"type":"INDIVIDUAL","first_name":"John","last_name":"Doe"},""" +
            """"beneficiary_address":{"line1":"123 Main St","line2":"Apt 4","city":"New York","region":"NY",""" +
            """"postal_code":"10001","country_code":"US"},"bank_name":"Chase Bank",""" +
            """"bank_address":{"line1":"123 Main St","line2":"Apt 4","city":"New York","region":"NY",""" +
            """"postal_code":"10001","country_code":"US"},"account_number":"1234567890",""" +
            """"routing_number":"021000021","bank_account_type":"SAVINGS"}}"""
    }
}