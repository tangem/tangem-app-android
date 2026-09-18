package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.cosign.SignatureProgressUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.wallet.presentation.wallet.state.model.JointOperationBannerUM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigInteger
import com.tangem.core.ui.R as CoreUiR

internal class JointOperationBannerConverterTest {

    private val clickedOperations = mutableListOf<JointPendingOperation>()

    private val converter = JointOperationBannerConverter(
        isBalanceHidden = false,
        onBannerClick = clickedOperations::add,
    )

    private val coin: CryptoCurrency.Coin = mockk {
        every { name } returns "Ethereum"
        every { symbol } returns "ETH"
        every { decimals } returns 18
        every { displayDecimals } returns 8
    }

    @Test
    fun `GIVEN no pending operation WHEN convert THEN no banners`() {
        // Act
        val actual = converter.convert(emptyList())

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN an operation from another member WHEN convert THEN the banner names them and the currency`() {
        // Act
        val actual = converter.convert(listOf(operation()))

        // Assert
        val banner = actual.single().banner
        assertThat(banner.title).isEqualTo(
            resourceReference(
                id = CoreUiR.string.joint_operation_banner_title,
                formatArgs = wrappedList(stringReference("Jacob"), "Ethereum"),
            ),
        )
        assertThat(banner.progress).isEqualTo(SignatureProgressUM(collected = 1, total = 3))
    }

    @Test
    fun `GIVEN this member created the operation WHEN convert THEN the banner speaks in the first person`() {
        // Act
        val actual = converter.convert(listOf(operation(isOwn = true, ownAnswer = Answer.CREATED)))

        // Assert
        assertThat(actual.single().banner.title).isEqualTo(
            resourceReference(
                id = CoreUiR.string.joint_operation_banner_title_own,
                formatArgs = wrappedList("Ethereum"),
            ),
        )
    }

    @Test
    fun `GIVEN a transfer WHEN convert THEN the recipient is shortened in the middle`() {
        // Act
        val actual = converter.convert(listOf(operation()))

        // Assert
        val subtitle = actual.single().banner.subtitle as TextReference.Res
        assertThat(subtitle.formatArgs.last()).isEqualTo("0x222...2222")
    }

    @Test
    fun `GIVEN several operations WHEN convert THEN each is announced and keyed by its own id`() {
        // Arrange
        val first = operation(operationId = "first")
        val second = operation(operationId = "second")

        // Act
        val actual = converter.convert(listOf(first, second))

        // Assert
        assertThat(actual.map(JointOperationBannerUM::operationId)).containsExactly("first", "second").inOrder()
    }

    @Test
    fun `GIVEN answered and unanswered operations WHEN convert THEN the unanswered ones come first`() {
        // Arrange
        val answered = operation(operationId = "answered", ownAnswer = Answer.SIGNED, collected = 2)
        val awaiting = operation(operationId = "awaiting", ownAnswer = Answer.WAITING, collected = 1)

        // Act
        val actual = converter.convert(listOf(answered, awaiting))

        // Assert
        assertThat(actual.map(JointOperationBannerUM::operationId)).containsExactly("awaiting", "answered").inOrder()
    }

    @Test
    fun `GIVEN two unanswered operations WHEN convert THEN the one closer to its threshold comes first`() {
        // Arrange
        val far = operation(operationId = "far", collected = 1)
        val close = operation(operationId = "close", collected = 2)

        // Act
        val actual = converter.convert(listOf(far, close))

        // Assert
        assertThat(actual.map(JointOperationBannerUM::operationId)).containsExactly("close", "far").inOrder()
    }

    @Test
    fun `GIVEN the roster does not name the initiator WHEN convert THEN the title falls back to their address`() {
        // Arrange
        val operation = operation(initiatorName = null, initiatorAddress = INITIATOR)

        // Act
        val actual = converter.convert(listOf(operation))

        // Assert
        assertThat(actual.single().banner.title)
            .isEqualTo(
                resourceReference(
                    id = CoreUiR.string.joint_operation_banner_title,
                    formatArgs = wrappedList(stringReference("0x333...3333"), "Ethereum"),
                ),
            )
    }

    /** The backend can report no initiator at all, and an empty actor would render the title as " wants to send" */
    @Test
    fun `GIVEN no initiator reported at all WHEN convert THEN the title says a member did it`() {
        // Arrange
        val operation = operation(initiatorName = null, initiatorAddress = null)

        // Act
        val actual = converter.convert(listOf(operation))

        // Assert
        assertThat(actual.single().banner.title).isEqualTo(
            resourceReference(
                id = CoreUiR.string.joint_operation_banner_title,
                formatArgs = wrappedList(
                    resourceReference(CoreUiR.string.joint_account_invite_members_member),
                    "Ethereum",
                ),
            ),
        )
    }

    /** With equal thresholds this degenerates to sorting by `collected`, which hides the intended rule */
    @Test
    fun `GIVEN unanswered operations with different thresholds WHEN convert THEN fewer signatures left comes first`() {
        // Arrange
        val far = operation(operationId = "far", collected = 3, threshold = 10)
        val close = operation(operationId = "close", collected = 2, threshold = 3)

        // Act
        val actual = converter.convert(listOf(far, close))

        // Assert
        assertThat(actual.map(JointOperationBannerUM::operationId)).containsExactly("close", "far").inOrder()
    }

    @Test
    fun `GIVEN a banner WHEN its onClick is invoked THEN the click callback receives that operation`() {
        // Arrange
        val target = operation(operationId = "target")

        // Act
        val actual = converter.convert(listOf(target))
        actual.single().banner.onClick?.invoke()

        // Assert
        assertThat(clickedOperations).containsExactly(target)
    }

    @Test
    fun `GIVEN the own owner address is unresolvable WHEN convert THEN the operation counts as unanswered`() {
        // Arrange
        val unresolvable = operation(operationId = "unresolvable", ownAnswer = null, collected = 1)
        val answered = operation(operationId = "answered", ownAnswer = Answer.SIGNED, collected = 2)

        // Act
        val actual = converter.convert(listOf(answered, unresolvable))

        // Assert
        assertThat(actual.map(JointOperationBannerUM::operationId))
            .containsExactly("unresolvable", "answered")
            .inOrder()
    }

    private fun operation(
        operationId: String = "27ca1f4e",
        isOwn: Boolean = false,
        ownAnswer: Answer? = Answer.WAITING,
        collected: Int = 1,
        threshold: Int = 3,
        initiatorName: String? = "Jacob",
        initiatorAddress: String? = null,
    ) = JointPendingOperation(
        account = mockk(),
        operationId = operationId,
        transfer = JointOperationTransfer(
            currency = coin,
            intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
        ),
        initiatorAddress = initiatorAddress,
        initiatorName = initiatorName,
        isOwn = isOwn,
        ownAnswer = ownAnswer,
        collected = collected,
        threshold = threshold,
    )

    private companion object {
        const val RECIPIENT = "0x2222222222222222222222222222222222222222"
        const val INITIATOR = "0x3333333333333333333333333333333333333333"
        val AMOUNT: BigInteger = BigInteger("500000000000000000")
    }
}