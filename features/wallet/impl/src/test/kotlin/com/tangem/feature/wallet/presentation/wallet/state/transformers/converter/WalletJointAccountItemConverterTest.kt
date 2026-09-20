package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_16
import com.tangem.core.ui.res.generated.icons.ic_user_plus_16
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.features.jointaccount.main.JointAccountMainUM
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * A joint account that is not active yet takes the joint-account feature's own row: the subtitle says where the
 * account stands, the balance is a plain zero, and the tap leads to the members screen.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletJointAccountItemConverterTest {

    private val userWalletId = UserWalletId("011")
    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 0, userWalletId = userWalletId)

    private val clickedAccounts = mutableListOf<Pair<UserWalletId, String>>()

    private val converter = createConverter(accountIdsWithInvites = emptySet())

    @BeforeEach
    fun resetClicks() {
        clickedAccounts.clear()
    }

    private fun createConverter(accountIdsWithInvites: Set<String>) = WalletJointAccountItemConverter(
        appCurrency = AppCurrency.Default,
        userWalletId = userWalletId,
        accountIdsWithInvites = accountIdsWithInvites,
        onAccountClick = { walletId, accountId -> clickedAccounts += walletId to accountId },
    )

    @Test
    fun `GIVEN pending status WHEN convert THEN members progress and zero balance without inviting`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Pending(StatusSource.ACTUAL, joinedMembers = 1, totalMembers = 3),
        )

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.accountId).isEqualTo(account.accountId.value)
        val state = actual.state as JointAccountMainUM.WaitingMembers
        assertThat(state.title).isEqualTo(stringReference("Joint #0"))
        assertThat(state.subtitle)
            .isEqualTo(resourceReference(R.string.joint_account_members_progress, wrappedList(1, 3)))
        assertThat(state.icon).isEqualTo(CryptoPortfolioIconConverter.convert(account.icon))
        assertThat(state.balance).isEqualTo(stringReference(zeroBalance()))
        assertThat(state.priceChange).isEqualTo(
            TangemPriceChange.State(
                value = stringReference(BigDecimal.ZERO.format { percent() }),
                direction = TangemPriceChange.Direction.Neutral,
            ),
        )
        assertThat(state.banner).isNull()
    }

    @Test
    fun `GIVEN invites on this device WHEN convert pending with free slots THEN the invite banner is offered`() {
        // Arrange
        val inviting = createConverter(accountIdsWithInvites = setOf(account.accountId.value))
        val status = createStatus(JointAccountStatusValue.Pending(StatusSource.ACTUAL, 1, 2))

        // Act
        val state = inviting.convert(status).state as JointAccountMainUM.WaitingMembers

        // Assert
        val banner = requireNotNull(state.banner)
        assertThat(banner.text).isEqualTo(resourceReference(R.string.joint_account_invite_members_to_join))
        assertThat(banner.icon).isEqualTo(Icons.ic_user_plus_16)

        // Act
        banner.onClick()

        // Assert
        assertThat(clickedAccounts).containsExactly(userWalletId to account.accountId.value)
    }

    @Test
    fun `GIVEN invites on this device WHEN convert pending with every slot taken THEN no banner is offered`() {
        // Arrange
        val inviting = createConverter(accountIdsWithInvites = setOf(account.accountId.value))
        val status = createStatus(JointAccountStatusValue.Pending(StatusSource.ACTUAL, joinedMembers = 3, totalMembers = 3))

        // Act
        val state = inviting.convert(status).state as JointAccountMainUM.WaitingMembers

        // Assert
        assertThat(state.banner).isNull()
    }

    @Test
    fun `GIVEN confirming status WHEN convert THEN waiting for activation without a banner for a member`() {
        // Arrange
        val status = createStatus(JointAccountStatusValue.Confirming(StatusSource.ACTUAL, totalMembers = 3))

        // Act
        val actual = converter.convert(status)

        // Assert
        val state = actual.state as JointAccountMainUM.WaitingMembers
        assertThat(state.subtitle).isEqualTo(resourceReference(R.string.joint_account_waiting_activation))
        assertThat(state.banner).isNull()
    }

    @Test
    fun `GIVEN invites on this device WHEN convert confirming THEN the activate banner is offered`() {
        // Arrange
        val inviting = createConverter(accountIdsWithInvites = setOf(account.accountId.value))
        val status = createStatus(JointAccountStatusValue.Confirming(StatusSource.ACTUAL, totalMembers = 3))

        // Act
        val state = inviting.convert(status).state as JointAccountMainUM.WaitingMembers

        // Assert
        val banner = requireNotNull(state.banner)
        assertThat(banner.text).isEqualTo(resourceReference(R.string.joint_account_activation_account_button_title))
        assertThat(banner.icon).isEqualTo(Icons.ic_clock_16)

        // Act
        banner.onClick()

        // Assert
        assertThat(clickedAccounts).containsExactly(userWalletId to account.accountId.value)
    }

    @Test
    fun `GIVEN loading and unavailable statuses WHEN convert THEN the matching feature states`() {
        // Act
        val loading = converter.convert(createStatus(JointAccountStatusValue.Loading))
        val unavailable = converter.convert(createStatus(JointAccountStatusValue.Error.Unavailable))

        // Assert
        assertThat(loading.state).isEqualTo(JointAccountMainUM.Loading)
        assertThat(unavailable.state).isEqualTo(JointAccountMainUM.TemporaryUnavailable)
    }

    @Test
    fun `GIVEN pending status WHEN row tapped THEN the members screen of the wallet is requested`() {
        // Arrange
        val status = createStatus(JointAccountStatusValue.Pending(StatusSource.ACTUAL, 1, 3))

        // Act
        (converter.convert(status).state as JointAccountMainUM.WaitingMembers).onClick()

        // Assert
        assertThat(clickedAccounts).containsExactly(userWalletId to account.accountId.value)
    }

    @Test
    fun `GIVEN active status WHEN convert THEN refused - the caller renders it as a regular portfolio`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Active(StatusSource.ACTUAL, totalMembers = 3, safeAddress = "0x5afe"),
        )

        // Act
        val actual = runCatching { converter.convert(status) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalStateException::class.java)
    }

    private fun createStatus(value: JointAccountStatusValue): AccountStatus.Joint {
        return AccountStatus.Joint(
            account = account,
            value = value,
            tokenList = TokenList.Empty,
            priceChangeLce = lceLoading(),
        )
    }

    private fun zeroBalance(): String {
        return BigDecimal.ZERO.format {
            fiat(fiatCurrencyCode = AppCurrency.Default.code, fiatCurrencySymbol = AppCurrency.Default.symbol)
        }
    }
}