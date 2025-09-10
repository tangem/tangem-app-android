package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveWalletAccountsResponseConverterTest {

    @Test
    fun convert() {
        // Arrange
        val userWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns UserWalletId("011")
        }

        val accountList = AccountList(
            userWallet = userWallet,
            accounts = setOf(Account.CryptoPortfolio.createMainAccount(userWalletId = userWallet.walletId)),
            totalAccounts = 1,
        )
            .getOrNull()!!

        // Act
        val actual = SaveWalletAccountsResponseConverter.convert(value = accountList)

        // Assert
        val expected = SaveWalletAccountsResponse(
            accounts = listOf(
                WalletAccountDTO(
                    id = accountList.mainAccount.accountId.value,
                    name = accountList.mainAccount.accountName.value,
                    derivationIndex = accountList.mainAccount.derivationIndex.value,
                    icon = accountList.mainAccount.icon.value.name,
                    iconColor = accountList.mainAccount.icon.color.name,
                ),
            ),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }
}