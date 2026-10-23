package com.tangem.data.txhistory.repository.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.jointaccount.models.JointOperationDto
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryItemDto
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationVoteEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationWithVotes
import org.junit.jupiter.api.Test

internal class JointOperationEntityConverterTest {

    private val converter = JointOperationEntityConverter()

    @Test
    fun `GIVEN a history item WHEN convert THEN the operation and one lowercase row per vote`() {
        // Arrange
        val item = JointOperationHistoryItemDto(
            operationId = "op",
            chainId = 1,
            safeTx = JointOperationDto.SafeTxDto(
                to = "0xRecipient",
                value = "10",
                data = "0x",
                operation = 0,
                safeTxGas = "0",
                baseGas = "1",
                gasPrice = "2",
                gasToken = "0x0",
                refundReceiver = "0xRelayer",
                nonce = "3",
            ),
            status = "cancelled",
            initiator = "0xAlice",
            createdAt = "2026-09-08T09:20:37.000Z",
            votes = listOf(
                JointOperationDto.VoteDto(address = "0xAlice", vote = "approve", votedAt = "2026-09-08T09:20:37.000Z"),
                JointOperationDto.VoteDto(address = "0xBob", vote = "reject", votedAt = null),
            ),
            cryptoAccountId = "account",
            safeAddress = "0xSafe",
            txHash = null,
            finishedAt = "2026-09-08T09:24:02.000Z",
        )

        // Act
        val actual = converter.convert(JointOperationEntityConverter.Input(userWalletId = WALLET_ID, dto = item))

        // Assert
        assertThat(actual).isEqualTo(
            JointOperationWithVotes(
                operation = JointOperationEntity(
                    operationId = "op",
                    userWalletId = WALLET_ID.stringValue,
                    cryptoAccountId = "account",
                    chainId = 1,
                    safeAddress = "0xsafe",
                    status = "cancelled",
                    initiator = "0xalice",
                    createdAt = "2026-09-08T09:20:37.000Z",
                    finishedAt = "2026-09-08T09:24:02.000Z",
                    txHash = null,
                    safeTx = JointOperationEntity.SafeTxEmbedded(
                        to = "0xRecipient",
                        value = "10",
                        data = "0x",
                        operation = 0,
                        safeTxGas = "0",
                        baseGas = "1",
                        gasPrice = "2",
                        gasToken = "0x0",
                        refundReceiver = "0xRelayer",
                        nonce = "3",
                    ),
                ),
                votes = listOf(
                    JointOperationVoteEntity(
                        operationId = "op",
                        userWalletId = WALLET_ID.stringValue,
                        address = "0xalice",
                        vote = "approve",
                        votedAt = "2026-09-08T09:20:37.000Z",
                    ),
                    JointOperationVoteEntity(
                        operationId = "op",
                        userWalletId = WALLET_ID.stringValue,
                        address = "0xbob",
                        vote = "reject",
                        votedAt = null,
                    ),
                ),
            ),
        )
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
    }
}