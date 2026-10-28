package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.toAvatarState
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.actionmessage.TangemActionMessage
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.core.ui.res.generated.icons.ic_clock_16
import com.tangem.core.ui.res.generated.icons.ic_user_plus_16
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * A joint account that is not active yet is one account row: the subtitle says where the account stands, the balance
 * is a plain zero, the action message under the heading names the next step, and the tap leads to the members screen.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletJointAccountItemConverterTest {

    private val userWalletId = UserWalletId("011")
    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 0, userWalletId = userWalletId)

    private val clickedAccounts = mutableListOf<Pair<UserWalletId, String>>()
    private val compromisedClicks = mutableListOf<UserWalletId>()

    private val converter = WalletJointAccountItemConverter(
        appCurrency = AppCurrency.Default,
        userWalletId = userWalletId,
        onAccountClick = { walletId, accountId -> clickedAccounts += walletId to accountId },
        onCompromisedAccountClick = { walletId -> compromisedClicks += walletId },
    )

    @BeforeEach
    fun resetClicks() {
        clickedAccounts.clear()
        compromisedClicks.clear()
    }


    @Test
    fun `GIVEN pending status WHEN convert THEN a not active row with members, zero balance and a waiting message`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Pending(StatusSource.ACTUAL, joinedMembers = 1, totalMembers = 3, threshold = 2),
        )

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual.id).isEqualTo(account.accountId.value)
        assertThat(actual.avatar).isEqualTo(account.toAvatarState())
        val state = actual.state as TangemAccountRow.State.NotActive
        assertThat(state.title).isEqualTo(stringReference("Joint #0"))
        assertThat(state.label).isNull()
        assertThat(state.description).isEqualTo(MEMBERS_THREE)
        assertThat(state.balance).isEqualTo(stringReference(zeroBalance()))
        assertThat(state.priceChange).isEqualTo(
            TangemPriceChange.State(
                value = stringReference(BigDecimal.ZERO.format { percent() }),
                direction = TangemPriceChange.Direction.Neutral,
            ),
        )
        val message = requireNotNull(state.actionMessage)
        assertThat(message.text).isEqualTo(
            pluralReference(R.plurals.joint_account_main_waiting_members, count = 2, formatArgs = wrappedList(2)),
        )
        assertThat(message.icon).isEqualTo(Icons.ic_clock_16)
        assertThat(message.variant).isEqualTo(TangemActionMessage.Variant.Warning)
    }

    @Test
    fun `GIVEN the account was created by this wallet WHEN convert pending with free slots THEN the invite message`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Pending(StatusSource.ACTUAL, 1, 2, threshold = 2, isOwnCreator = true),
        )

        // Act
        val state = converter.convert(status).state as TangemAccountRow.State.NotActive

        // Assert
        val message = requireNotNull(state.actionMessage)
        assertThat(message.text).isEqualTo(resourceReference(R.string.joint_account_invite_members_to_join))
        assertThat(message.icon).isEqualTo(Icons.ic_user_plus_16)

        // Act
        message.onClick()

        // Assert
        assertThat(clickedAccounts).containsExactly(userWalletId to account.accountId.value)
    }

    @Test
    fun `GIVEN the account was created by this wallet WHEN convert pending with every slot taken THEN no message`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Pending(
                source = StatusSource.ACTUAL,
                joinedMembers = 3,
                totalMembers = 3,
                threshold = 2,
                isOwnCreator = true,
            ),
        )

        // Act
        val state = converter.convert(status).state as TangemAccountRow.State.NotActive

        // Assert
        assertThat(state.actionMessage).isNull()
    }

    @Test
    fun `GIVEN confirming status not confirmed by this wallet WHEN convert THEN the confirm message`() {
        // Arrange
        val status = createStatus(JointAccountStatusValue.Confirming(StatusSource.ACTUAL, totalMembers = 3, threshold = 2))

        // Act
        val state = converter.convert(status).state as TangemAccountRow.State.NotActive

        // Assert
        assertThat(state.description).isEqualTo(MEMBERS_THREE)
        val message = requireNotNull(state.actionMessage)
        assertThat(message.text).isEqualTo(resourceReference(R.string.joint_account_confirm_shared_address))
        assertThat(message.icon).isEqualTo(Icons.ic_checkmark_16)

        // Act
        message.onClick()

        // Assert
        assertThat(clickedAccounts).containsExactly(userWalletId to account.accountId.value)
    }

    @Test
    fun `GIVEN confirming status confirmed by this wallet WHEN convert THEN waiting for the others' confirmations`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Confirming(
                source = StatusSource.ACTUAL,
                totalMembers = 3,
                verifiedMembers = 1,
                isOwnVerified = true,
                threshold = 2,
            ),
        )

        // Act
        val state = converter.convert(status).state as TangemAccountRow.State.NotActive

        // Assert
        val message = requireNotNull(state.actionMessage)
        assertThat(message.text).isEqualTo(
            pluralReference(R.plurals.joint_account_main_waiting_confirmations, count = 2, formatArgs = wrappedList(2)),
        )
        assertThat(message.icon).isEqualTo(Icons.ic_clock_16)
    }

    @Test
    fun `GIVEN loading status WHEN convert THEN a shimmer row`() {
        // Act
        val actual = converter.convert(createStatus(JointAccountStatusValue.Loading))

        // Assert
        assertThat(actual.state).isEqualTo(TangemAccountRow.State.Shimmer(id = account.accountId.value))
    }

    @Test
    fun `GIVEN unavailable status WHEN convert THEN a balance error row that cannot be tapped`() {
        // Act
        val actual = converter.convert(createStatus(JointAccountStatusValue.Error.Unavailable))

        // Assert
        assertThat(actual.state).isEqualTo(
            TangemAccountRow.State.BalanceError(id = account.accountId.value, title = stringReference("Joint #0")),
        )
    }

    @Test
    fun `GIVEN compromised status WHEN convert THEN the badge takes the place of the balance`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Error.Compromised(
                totalMembers = 5,
                tokensCount = 6,
                fiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            ),
        )

        // Act
        val state = converter.convert(status).state as TangemAccountRow.State.Unreachable

        // Assert
        assertThat(state.title).isEqualTo(stringReference("Joint #0"))
        assertThat(state.badge).isEqualTo(resourceReference(R.string.joint_account_main_cant_verify))
        assertThat(state.label).isEqualTo(
            pluralReference(R.plurals.common_tokens_count, count = 6, formatArgs = wrappedList(6)),
        )
        assertThat(state.description).isEqualTo(
            pluralReference(R.plurals.joint_account_members_count, count = 5, formatArgs = wrappedList(5)),
        )
    }

    @Test
    fun `GIVEN compromised status WHEN row tapped THEN the alert is requested instead of the members screen`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Error.Compromised(
                totalMembers = 5,
                tokensCount = 6,
                fiatBalance = TotalFiatBalance.Loading,
            ),
        )

        // Act
        requireNotNull((converter.convert(status).state as TangemAccountRow.State.Unreachable).onClick).invoke()

        // Assert
        assertThat(compromisedClicks).containsExactly(userWalletId)
        assertThat(clickedAccounts).isEmpty()
    }

    @Test
    fun `GIVEN pending status WHEN row tapped THEN the members screen of the wallet is requested`() {
        // Arrange
        val status = createStatus(JointAccountStatusValue.Pending(StatusSource.ACTUAL, 1, 3, threshold = 2))

        // Act
        requireNotNull((converter.convert(status).state as TangemAccountRow.State.NotActive).onClick).invoke()

        // Assert
        assertThat(clickedAccounts).containsExactly(userWalletId to account.accountId.value)
    }

    @Test
    fun `GIVEN active status WHEN convert THEN refused - the caller renders it as a regular portfolio`() {
        // Arrange
        val status = createStatus(
            JointAccountStatusValue.Active(
                StatusSource.ACTUAL,
                totalMembers = 3,
                safeAddress = "0x5afe",
                threshold = 2,
            ),
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

    private companion object {
        val MEMBERS_THREE = pluralReference(R.plurals.joint_account_members_count, count = 3, formatArgs = wrappedList(3))
    }
}