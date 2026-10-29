package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.data.common.account.toWalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveWalletAccountsResponseConverterTest {

    @Test
    fun convert() {
        // Arrange
        val userWalletId = UserWalletId("011")
        val accountList = createAccountList(userWalletId)

        // Act
        val actual = SaveWalletAccountsResponseConverter.convert(value = accountList)

        // Assert
        val expected = SaveWalletAccountsResponse(
            accounts = listOf(
                SaveWalletAccountsResponse.AccountDTO(
                    id = accountList.mainAccount.accountId.value,
                    name = (accountList.mainAccount.accountName as? AccountName.Custom)?.value,
                    derivationIndex = accountList.mainAccount.derivationIndex.value,
                    icon = accountList.mainAccount.icon.value.name,
                    iconColor = accountList.mainAccount.icon.color.name,
                    type = WalletAccountDTO.Type.CRYPTO,
                ),
            ),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    /** The collectibles row goes back in every save: a row the body leaves out is archived by the backend */
    @Test
    fun `GIVEN a collectibles account WHEN convert THEN its row is in the body`() {
        // Arrange
        val userWalletId = UserWalletId("011")
        val collectibles = Account.Collectibles(userWalletId = userWalletId)
        val accountList = createAccountList(userWalletId).let { list ->
            list.withAccounts(list.accounts + collectibles).getOrNull()!!
        }

        // Act
        val actual = SaveWalletAccountsResponseConverter.convert(value = accountList)

        // Assert
        val record = collectibles.toWalletAccountDTO()
        Truth.assertThat(actual.accounts).contains(
            SaveWalletAccountsResponse.AccountDTO(
                id = record.id,
                name = record.name,
                derivationIndex = record.derivationIndex,
                icon = record.icon,
                iconColor = record.iconColor,
                type = WalletAccountDTO.Type.COLLECTIBLES,
            ),
        )
    }
}