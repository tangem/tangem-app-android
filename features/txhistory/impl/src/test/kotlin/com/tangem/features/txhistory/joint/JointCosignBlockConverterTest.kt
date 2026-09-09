package com.tangem.features.txhistory.joint

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.cosign.SignatureProgressUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import com.tangem.core.ui.R as CoreUiR

internal class JointCosignBlockConverterTest {

    private val converter = JointCosignBlockConverter(onSignClick = {})

    @Test
    fun `GIVEN no operation behind the row WHEN convert THEN the details are unchanged`() {
        // Arrange
        val state = singleAsset()

        // Act
        val actual = converter.convert(input(state = state, cosign = null))

        // Assert
        assertThat(actual).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN a two-asset layout WHEN convert THEN it is unchanged`() {
        // Arrange
        val state: TxHistoryDetailsUM.TwoAssets = mockk()

        // Act
        val actual = converter.convert(input(state = state, cosign = cosignState()))

        // Assert
        assertThat(actual).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN an operation collecting signatures WHEN convert THEN the pending header and the signatures row`() {
        // Act
        val actual = converter.convert(input(state = singleAsset(), cosign = cosignState()))

        // Assert
        val content = actual as TxHistoryDetailsUM.SingleAsset
        assertThat(content.header.subtitleStyle).isEqualTo(TxHistoryDetailsUM.HeaderUM.SubtitleStyle.Warning)
        assertThat(content.cosign?.progress).isEqualTo(SignatureProgressUM(collected = 2, total = 3))
        assertThat(content.cosignAction?.isEnabled).isTrue()
        assertThat(content.cosignAction?.isLoading).isFalse()
    }

    @Test
    fun `GIVEN this member already answered WHEN convert THEN the Sign action is disabled`() {
        // Act
        val actual = converter.convert(input(state = singleAsset(), cosign = cosignState(canSign = false)))

        // Assert
        assertThat((actual as TxHistoryDetailsUM.SingleAsset).cosignAction?.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN the signing is in flight WHEN convert THEN the Sign action is loading`() {
        // Act
        val actual = converter.convert(input(state = singleAsset(), cosign = cosignState(), isSigning = true))

        // Assert
        assertThat((actual as TxHistoryDetailsUM.SingleAsset).cosignAction?.isLoading).isTrue()
    }

    @Test
    fun `GIVEN this member declined WHEN convert THEN the action reads the answer back with the reachable threshold`() {
        // Act
        val actual = converter.convert(
            input(state = singleAsset(), cosign = cosignState(canSign = false, ownAnswer = Answer.DECLINED)),
        )

        // Assert
        val action = (actual as TxHistoryDetailsUM.SingleAsset).cosignAction
        assertThat(action?.buttonText).isEqualTo(resourceReference(CoreUiR.string.joint_operation_declined))
        assertThat(action?.caption).isEqualTo(
            resourceReference(
                id = CoreUiR.string.joint_operation_declined_footer,
                formatArgs = wrappedList(2, 3),
            ),
        )
        assertThat(action?.isEnabled).isFalse()
        assertThat(action?.iconEnd).isNull()
    }

    @Test
    fun `GIVEN this member signed WHEN convert THEN the action reads the answer back and still promises the send`() {
        // Act
        val actual = converter.convert(
            input(state = singleAsset(), cosign = cosignState(canSign = false, ownAnswer = Answer.SIGNED)),
        )

        // Assert
        val action = (actual as TxHistoryDetailsUM.SingleAsset).cosignAction
        assertThat(action?.buttonText).isEqualTo(resourceReference(CoreUiR.string.joint_operation_signed))
        assertThat(action?.caption).isEqualTo(
            resourceReference(
                id = CoreUiR.string.joint_operation_signed_footer,
                formatArgs = wrappedList(2, 3),
            ),
        )
        assertThat(action?.isEnabled).isFalse()
        assertThat(action?.iconEnd).isNull()
    }

    @Test
    fun `GIVEN the signature was just given WHEN convert THEN the action reads Signed before the answer arrives`() {
        // Act
        val actual = converter.convert(
            input(state = singleAsset(), cosign = cosignState(), hasAnswered = true),
        )

        // Assert
        val action = (actual as TxHistoryDetailsUM.SingleAsset).cosignAction
        assertThat(action?.buttonText).isEqualTo(resourceReference(CoreUiR.string.joint_operation_signed))
        assertThat(action?.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN this member cannot sign and never answered WHEN convert THEN the Sign action is only disabled`() {
        // Act
        val actual = converter.convert(input(state = singleAsset(), cosign = cosignState(canSign = false)))

        // Assert
        val action = (actual as TxHistoryDetailsUM.SingleAsset).cosignAction
        assertThat(action?.buttonText).isEqualTo(resourceReference(CoreUiR.string.common_sign))
        assertThat(action?.caption).isEqualTo(
            resourceReference(
                id = CoreUiR.string.joint_operation_auto_sends_footer,
                formatArgs = wrappedList(2, 3),
            ),
        )
    }

    @Test
    fun `GIVEN an operation behind the row WHEN convert THEN the header menu is dropped`() {
        // Act
        val actual = converter.convert(input(state = singleAsset(menu = menu()), cosign = cosignState()))

        // Assert
        assertThat((actual as TxHistoryDetailsUM.SingleAsset).header.menu).isEmpty()
    }

    @Test
    fun `GIVEN no operation behind the row WHEN convert THEN the header menu is kept`() {
        // Arrange
        val state = singleAsset(menu = menu())

        // Act
        val actual = converter.convert(input(state = state, cosign = null))

        // Assert
        assertThat((actual as TxHistoryDetailsUM.SingleAsset).header.menu).hasSize(1)
    }

    private fun input(
        state: TxHistoryDetailsUM?,
        cosign: JointCosignState?,
        isSigning: Boolean = false,
        hasAnswered: Boolean = false,
    ) = JointCosignBlockConverter.Input(
        state = state,
        cosign = cosign,
        isSigning = isSigning,
        hasAnswered = hasAnswered,
    )

    private fun singleAsset(menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM> = persistentListOf()) =
        TxHistoryDetailsUM.SingleAsset(
        header = TxHistoryDetailsUM.HeaderUM(
            icon = mockk(),
            status = TransactionItemUM.Content.Status.Unconfirmed,
            title = stringReference("Send"),
            subtitle = stringReference("Today, 15:34"),
            menu = menu,
        ),
        isBalanceHidden = false,
        amountBlock = mockk(),
        counterparty = null,
        rows = persistentListOf(),
    )

    private fun menu() = persistentListOf(
        TxHistoryDetailsUM.MenuItemUM(
            icon = mockk(),
            title = stringReference("Explore"),
            action = TxHistoryDetailsUM.MenuItemUM.Action.Direct(onClick = {}),
        ),
    )

    /** The initiator plus one co-signer have answered, so the ring shows two of the three members */
    private fun cosignState(canSign: Boolean = true, ownAnswer: Answer? = null) = JointCosignState(
        account = mockk(),
        displayed = mockk(),
        threshold = 2,
        membersCount = 3,
        signers = listOf(
            signer(name = "Isaac", answer = Answer.CREATED),
            signer(name = "Kingston", answer = Answer.SIGNED),
            signer(name = "Daniel", answer = Answer.WAITING),
        ),
        canSign = canSign,
        ownAnswer = ownAnswer,
    )

    private fun signer(name: String, answer: Answer) = JointOperationSignerState(
        member = JointAccount.Member(name = name, address = "0x$name", role = JointAccount.Role.MEMBER),
        answer = answer,
        at = null,
    )
}