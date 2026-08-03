package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.CustomerMeResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CustomerInfoConverterKycTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: KycModel) {
        // Act
        val info = CustomerInfoConverter.convert(result(model.kyc))

        // Assert
        assertThat(KycFields(country = info.country, phoneMask = info.phoneMask, email = info.email))
            .isEqualTo(model.expected)
    }

    internal data class KycModel(val kyc: CustomerMeResponse.Kyc?, val expected: KycFields)

    internal data class KycFields(val country: String?, val phoneMask: String?, val email: String?)

    private fun provideTestModels() = listOf(
        KycModel(
            kyc = kyc(country = "US", phoneMask = "+1 (###) ###-####", email = "a@b.co"),
            expected = KycFields(country = "US", phoneMask = "+1 (###) ###-####", email = "a@b.co"),
        ),
        KycModel(
            kyc = null,
            expected = KycFields(country = null, phoneMask = null, email = null),
        ),
        KycModel(
            kyc = kyc(country = null, phoneMask = null),
            expected = KycFields(country = null, phoneMask = null, email = null),
        ),
    )

    private fun result(kyc: CustomerMeResponse.Kyc?) = CustomerMeResponse.Result(
        id = "c1",
        state = "ACTIVE",
        createdAt = "2026-01-01T00:00:00Z",
        paymentAccount = null,
        kyc = kyc,
        depositAddress = null,
        balance = null,
        productInstances = emptyList(),
        cards = emptyList(),
        customerTariffPlan = null,
    )

    private fun kyc(country: String?, phoneMask: String?, email: String? = null) = CustomerMeResponse.Kyc(
        id = "k1",
        provider = "provider",
        status = "APPROVED",
        risk = "LOW",
        reviewAnswer = "GREEN",
        createdAt = "2026-01-01T00:00:00Z",
        country = country,
        phoneMask = phoneMask,
        email = email,
    )
}