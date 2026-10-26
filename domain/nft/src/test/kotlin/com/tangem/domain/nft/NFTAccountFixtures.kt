package com.tangem.domain.nft

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.mock.MockAccounts

internal object NFTAccountFixtures {

    val userWalletId = UserWalletId("011")

    val ethereum: CryptoCurrency.Coin = MockCryptoCurrencyFactory().ethereum
    val jointEthereum: CryptoCurrency.Coin = ethereum.onJointPath()

    val personalAccount: Account.Personal = MockAccounts.createAccount(
        derivationIndex = 1,
        cryptoCurrencies = listOf(ethereum),
        userWalletId = userWalletId,
    )
    val jointAccount: Account.Joint = MockAccounts.createJointAccount(
        cryptoCurrencies = listOf(jointEthereum),
        userWalletId = userWalletId,
    )

    val accountList: AccountList = AccountList(
        userWalletId = userWalletId,
        accounts = listOf(Account.Personal.createMainAccount(userWalletId), personalAccount, jointAccount),
        totalAccounts = 2,
        totalArchivedAccounts = 0,
        totalJointAccounts = 1,
    ).getOrNull()!!

    private fun CryptoCurrency.Coin.onJointPath(): CryptoCurrency.Coin {
        val jointPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")

        return copy(
            network = network.copy(
                id = network.id.copy(derivationPath = jointPath),
                derivationPath = jointPath,
            ),
        )
    }
}