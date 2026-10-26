package com.tangem.data.account.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the stored format of the wallet accounts response: files written by the Moshi serializer must read back
 * unchanged after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still
 * used while the kotlinx DataStore toggle is off reads them too.
 *
 * Values are also compared by `toString`, because [UserTokensResponse.Token.equals] ignores most of its fields.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountsResponseStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN response WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<GetWalletAccountsResponse?>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN response is kept`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<GetWalletAccountsResponse?>().fromJson(model.json)

        // Assert
        assertThat(actual.toString()).isEqualTo(model.value.toString())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN response is kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson<GetWalletAccountsResponse?>(json = model.json, defaultValue = DEFAULT)

        // Assert
        assertThat(actual.toString()).isEqualTo(model.value.toString())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN response WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson<GetWalletAccountsResponse?>(value = model.value, defaultValue = DEFAULT)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    version = 3,
                    group = UserTokensResponse.GroupType.NETWORK,
                    sort = UserTokensResponse.SortType.BALANCE,
                    totalAccounts = 1,
                    totalArchivedAccounts = 2,
                    totalJointAccounts = 0,
                ),
                accounts = listOf(ACCOUNT),
                unassignedTokens = listOf(TOKEN.copy(id = null, contractAddress = null, addresses = null)),
            ),
            json = """{"wallet":{"version":3,"group":"network","sort":"balance","totalAccounts":1,""" +
                """"totalArchivedAccounts":2,"totalJointAccounts":0},"accounts":[$ACCOUNT_JSON],""" +
                """"unassignedTokens":[$TOKEN_MINIMAL_JSON]}""",
        ),
        FormatModel(
            value = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    group = null,
                    sort = null,
                    totalAccounts = 1,
                    totalArchivedAccounts = 0,
                ),
                accounts = listOf(ACCOUNT.copy(name = null, type = null, tokens = null)),
                unassignedTokens = emptyList(),
            ),
            json = """{"wallet":{"version":0,"totalAccounts":1,"totalArchivedAccounts":0},""" +
                """"accounts":[{"id":"account","derivation":0,"icon":"letter","iconColor":"blue",""" +
                """"totalTokens":1,"totalNetworks":1}],"unassignedTokens":[]}""",
        ),
        FormatModel(value = null, json = "null"),
    )

    data class FormatModel(val value: GetWalletAccountsResponse?, val json: String)

    private companion object {
        val DEFAULT = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = null,
                sort = null,
                totalAccounts = 0,
                totalArchivedAccounts = 0,
            ),
            accounts = emptyList(),
            unassignedTokens = emptyList(),
        )
        val TOKEN = UserTokensResponse.Token(
            id = "tether",
            accountId = "account",
            networkId = "ethereum",
            derivationPath = "m/44'/60'/0'/0/0",
            name = "Tether",
            symbol = "USDT",
            decimals = 6,
            contractAddress = "0xtoken",
            addresses = listOf("0xaddress"),
            dynamicAddressesEnabled = true,
        )
        val ACCOUNT = WalletAccountDTO(
            id = "account",
            name = "Main",
            derivationIndex = 0,
            icon = "letter",
            iconColor = "blue",
            type = WalletAccountDTO.Type.CRYPTO,
            tokens = listOf(TOKEN),
            totalTokens = 1,
            totalNetworks = 1,
        )
        const val TOKEN_JSON = """{"id":"tether","accountId":"account","networkId":"ethereum",""" +
            """"derivationPath":"m/44'/60'/0'/0/0","name":"Tether","symbol":"USDT","decimals":6,""" +
            """"contractAddress":"0xtoken","addresses":["0xaddress"],"dynamicAddressesEnabled":true}"""
        const val TOKEN_MINIMAL_JSON = """{"accountId":"account","networkId":"ethereum",""" +
            """"derivationPath":"m/44'/60'/0'/0/0","name":"Tether","symbol":"USDT","decimals":6,""" +
            """"dynamicAddressesEnabled":true}"""
        const val ACCOUNT_JSON = """{"id":"account","name":"Main","derivation":0,"icon":"letter",""" +
            """"iconColor":"blue","type":"crypto","tokens":[$TOKEN_JSON],"totalTokens":1,"totalNetworks":1}"""
    }
}