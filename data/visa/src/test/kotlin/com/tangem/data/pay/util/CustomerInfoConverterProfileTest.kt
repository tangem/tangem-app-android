package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.CustomerMeResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CustomerInfoConverterProfileTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ProfileModel) {
        // Act
        val info = CustomerInfoConverter.convert(result(profile = model.profile))

        // Assert
        assertThat(ProfileFields(country = info.country, phoneMask = info.phoneMask, email = info.email))
            .isEqualTo(model.expected)
    }

    internal data class ProfileModel(
        val profile: CustomerMeResponse.Profile?,
        val expected: ProfileFields,
    )

    internal data class ProfileFields(val country: String?, val phoneMask: String?, val email: String?)

    private fun provideTestModels() = listOf(
        ProfileModel(
            profile = profile(country = "US", phoneMask = "+1 ###-###-####", email = "a@b.co"),
            expected = ProfileFields(country = "US", phoneMask = "+1 ###-###-####", email = "a@b.co"),
        ),
        ProfileModel(
            profile = null,
            expected = ProfileFields(country = null, phoneMask = null, email = null),
        ),
        ProfileModel(
            profile = profile(country = "DE", phoneMask = null, email = null),
            expected = ProfileFields(country = "DE", phoneMask = null, email = null),
        ),
    )

    private fun result(profile: CustomerMeResponse.Profile?) = CustomerMeResponse.Result(
        id = "c1",
        state = "ACTIVE",
        createdAt = "2026-01-01T00:00:00Z",
        paymentAccount = null,
        kyc = null,
        depositAddress = null,
        balance = null,
        productInstances = emptyList(),
        cards = emptyList(),
        customerTariffPlan = null,
        profile = profile,
    )

    private fun profile(country: String?, phoneMask: String?, email: String?) = CustomerMeResponse.Profile(
        country = country,
        phoneMask = phoneMask,
        email = email,
    )
}