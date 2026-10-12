package com.tangem.domain.models.account

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the JSON format of [AccountId] now that it has a generated Moshi adapter. The expected strings are
 * what the reflective `KotlinJsonAdapterFactory` wrote before, so ids already stored on devices (e.g. in
 * WalletConnect sessions) must still read back unchanged. The Moshi instance has no reflective factory on
 * purpose: without the generated adapter it could not serialize the class at all.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountIdJsonAdapterTest {

    private val adapter = Moshi.Builder().build().adapter(AccountId::class.java)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN json in the stored format WHEN fromJson THEN reads the same id`(model: FormatModel) {
        // Act
        val actual = adapter.fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.accountId)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN account id WHEN toJson THEN writes the stored format`(model: FormatModel) {
        // Act
        val actual = adapter.toJson(model.accountId)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            accountId = AccountId.forMainCryptoPortfolio(userWalletId = USER_WALLET_ID),
            json = """{"value":"4E39B13EA11E3B35339664A10BEF48F4AF752A1CC2200F79D23CB0FB3396C63F",""" +
                """"userWalletId":{"stringValue":"$WALLET_ID"}}""",
        ),
        // Non-hex values never passed the public factories' validation; the reflective adapter read them
        // through the primary constructor, and so does the generated one
        FormatModel(
            accountId = AccountId.forPaymentAccount(userWalletId = USER_WALLET_ID),
            json = """{"value":"payment_UserWalletId(271...C8F)","userWalletId":{"stringValue":"$WALLET_ID"}}""",
        ),
    )

    data class FormatModel(val accountId: AccountId, val json: String)

    private companion object {
        const val WALLET_ID = "27163F47405CE73110837F24DF82607FF11C7AF9D78C93F409E4FEAFF3400C8F"
        val USER_WALLET_ID = UserWalletId(WALLET_ID)
    }
}