package com.tangem.features.send.subcomponents.destination.model.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.send.subcomponents.destination.ui.state.DestinationWalletUM
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test

internal class JointAccountDestinationDeduplicatorTest {

    private val currency = MockCryptoCurrencyFactory().ethereum

    @Test
    fun `GIVEN two local members of one joint account WHEN deduplicate THEN the account is offered once`() {
        // Arrange
        val first = joint(walletId = WALLET_A)
        val second = joint(walletId = WALLET_B)

        // Act
        val actual = JointAccountDestinationDeduplicator.deduplicate(
            destinations = listOf(first, second),
            senderWalletId = OTHER_WALLET,
        )

        // Assert
        assertThat(actual).containsExactly(first)
    }

    @Test
    fun `GIVEN the sender is one of the members WHEN deduplicate THEN its own entry is kept in place`() {
        // Arrange
        val personal = personal(walletId = WALLET_A, address = OTHER_ADDRESS)
        val first = joint(walletId = WALLET_A)
        val sendersEntry = joint(walletId = WALLET_B)

        // Act
        val actual = JointAccountDestinationDeduplicator.deduplicate(
            destinations = listOf(personal, first, sendersEntry),
            senderWalletId = WALLET_B,
        )

        // Assert
        assertThat(actual).containsExactly(personal, sendersEntry).inOrder()
    }

    @Test
    fun `GIVEN the same Safe address in another case WHEN deduplicate THEN the account is offered once`() {
        // Arrange
        val first = joint(walletId = WALLET_A, address = SAFE_ADDRESS.lowercase())
        val second = joint(walletId = WALLET_B, address = SAFE_ADDRESS)

        // Act
        val actual = JointAccountDestinationDeduplicator.deduplicate(
            destinations = listOf(first, second),
            senderWalletId = OTHER_WALLET,
        )

        // Assert
        assertThat(actual).containsExactly(first)
    }

    @Test
    fun `GIVEN two different joint accounts WHEN deduplicate THEN both are offered`() {
        // Arrange
        val first = joint(walletId = WALLET_A)
        val second = joint(walletId = WALLET_A, address = OTHER_ADDRESS)

        // Act
        val actual = JointAccountDestinationDeduplicator.deduplicate(
            destinations = listOf(first, second),
            senderWalletId = OTHER_WALLET,
        )

        // Assert
        assertThat(actual).containsExactly(first, second).inOrder()
    }

    @Test
    fun `GIVEN personal wallets sharing an address WHEN deduplicate THEN they are left alone`() {
        // Arrange
        val first = personal(walletId = WALLET_A, address = SAFE_ADDRESS)
        val second = personal(walletId = WALLET_B, address = SAFE_ADDRESS)

        // Act
        val actual = JointAccountDestinationDeduplicator.deduplicate(
            destinations = listOf(first, second),
            senderWalletId = OTHER_WALLET,
        )

        // Assert
        assertThat(actual).containsExactly(first, second).inOrder()
    }

    private fun joint(walletId: UserWalletId, address: String = SAFE_ADDRESS) = destination(
        walletId = walletId,
        address = address,
        account = MockAccounts.createJointAccount(userWalletId = walletId),
    )

    private fun personal(walletId: UserWalletId, address: String) = destination(
        walletId = walletId,
        address = address,
        account = Account.Personal.createMainAccount(walletId),
    )

    private fun destination(walletId: UserWalletId, address: String, account: Account) = DestinationWalletUM(
        name = "Wallet ${walletId.stringValue}",
        userWalletId = walletId,
        address = address,
        cryptoCurrency = currency,
        account = account,
    )

    private companion object {
        val WALLET_A = UserWalletId("0A")
        val WALLET_B = UserWalletId("0B")
        val OTHER_WALLET = UserWalletId("0C")
        const val SAFE_ADDRESS = "0xAbCdEf0123456789aBcDeF0123456789AbCdEf01"
        const val OTHER_ADDRESS = "0x1111111111111111111111111111111111111111"
    }
}