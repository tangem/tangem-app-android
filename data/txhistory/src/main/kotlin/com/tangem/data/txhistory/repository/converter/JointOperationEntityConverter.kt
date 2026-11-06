package com.tangem.data.txhistory.repository.converter

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryItemDto
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationVoteEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationWithVotes
import com.tangem.utils.converter.Converter

internal class JointOperationEntityConverter :
    Converter<JointOperationEntityConverter.Input, JointOperationWithVotes> {

    override fun convert(value: Input): JointOperationWithVotes {
        val dto = value.dto
        return JointOperationWithVotes(
            operation = JointOperationEntity(
                operationId = dto.operationId,
                userWalletId = value.userWalletId.stringValue,
                cryptoAccountId = dto.cryptoAccountId,
                chainId = dto.chainId,
                safeAddress = dto.safeAddress.lowercase(),
                status = dto.status,
                initiator = dto.initiator?.lowercase(),
                createdAt = dto.createdAt,
                finishedAt = dto.finishedAt,
                txHash = dto.txHash?.lowercase(),
                safeTx = with(dto.safeTx) {
                    JointOperationEntity.SafeTxEmbedded(
                        to = to,
                        value = this.value,
                        data = data,
                        operation = operation,
                        safeTxGas = safeTxGas,
                        baseGas = baseGas,
                        gasPrice = gasPrice,
                        gasToken = gasToken,
                        refundReceiver = refundReceiver,
                        nonce = nonce,
                    )
                },
            ),
            votes = dto.votes.map { vote ->
                JointOperationVoteEntity(
                    operationId = dto.operationId,
                    userWalletId = value.userWalletId.stringValue,
                    address = vote.address.lowercase(),
                    vote = vote.vote,
                    votedAt = vote.votedAt,
                )
            },
        )
    }

    data class Input(val userWalletId: UserWalletId, val dto: JointOperationHistoryItemDto)
}