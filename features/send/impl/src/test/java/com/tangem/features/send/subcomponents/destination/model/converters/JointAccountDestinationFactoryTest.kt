package com.tangem.features.send.subcomponents.destination.model.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.send.subcomponents.destination.ui.state.DestinationWalletUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountDestinationFactoryTest {

    private val userWallet: UserWallet = MockUserWalletFactory.create()
    private val currency = MockCryptoCurrencyFactory().ethereum
    private val jointAccount = MockAccounts.createJointAccount(ownerKeyIndex = 0, userWalletId = userWallet.walletId)

    @Test
    fun `GIVEN active account in a supported network WHEN create THEN the Safe address is offered`() {
        // Act
        val actual = JointAccountDestinationFactory.create(
            accountStatus = jointStatus(value = activeValue()),
            wallet = userWallet,
            cryptoCurrency = currency,
            supportedNetworks = listOf(currency.network.id.rawId),
        )

        // Assert
        assertThat(actual).containsExactly(
            DestinationWalletUM(
                name = userWallet.name,
                userWalletId = userWallet.walletId,
                address = SAFE_ADDRESS,
                cryptoCurrency = currency,
                account = jointAccount,
            ),
        )
    }

    @Test
    fun `GIVEN active account in an unsupported network WHEN create THEN nothing is offered`() {
        // Act
        // Coins sent here would land on an address that can never spend them: no canonical Safe in this network
        val actual = JointAccountDestinationFactory.create(
            accountStatus = jointStatus(value = activeValue()),
            wallet = userWallet,
            cryptoCurrency = currency,
            supportedNetworks = listOf(Network.RawID(value = "solana")),
        )

        // Assert
        assertThat(actual).isEmpty()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun create(model: NotActiveModel) {
        // Act
        val actual = JointAccountDestinationFactory.create(
            accountStatus = jointStatus(value = model.value),
            wallet = userWallet,
            cryptoCurrency = currency,
            supportedNetworks = listOf(currency.network.id.rawId),
        )

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun provideTestModels() = listOf(
        // Slots are still being filled - the Safe address does not exist yet
        NotActiveModel(JointAccountStatusValue.Pending(StatusSource.ACTUAL, joinedMembers = 1, totalMembers = 3)),
        // The address exists, but until the creator activates the account it accepts nothing
        NotActiveModel(JointAccountStatusValue.Confirming(StatusSource.ACTUAL, totalMembers = 3)),
        NotActiveModel(JointAccountStatusValue.Loading),
        NotActiveModel(JointAccountStatusValue.Error.Unavailable),
    )

    internal data class NotActiveModel(val value: JointAccountStatusValue)

    private fun activeValue() = JointAccountStatusValue.Active(
        source = StatusSource.ACTUAL,
        totalMembers = 3,
        safeAddress = SAFE_ADDRESS,
    )

    private fun jointStatus(value: JointAccountStatusValue) = AccountStatus.Joint(
        account = jointAccount,
        value = value,
        tokenList = TokenList.Empty,
        priceChangeLce = lceLoading(),
    )

    private companion object {
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
    }
}