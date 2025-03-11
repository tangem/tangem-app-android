package com.tangem.blockchainsdk.accountcreator

import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.toHexString
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CreateUserNetworkAccountBody

internal class DefaultAccountCreator(
    private val tangemTechApi: TangemTechApi,
) : AccountCreator {

    override suspend fun createAccount(blockchain: Blockchain, walletPublicKey: ByteArray): Result<String> {
        val request = CreateUserNetworkAccountBody(
            networkId = blockchain.id.removeSuffix("/test"),
            walletPublicKey = walletPublicKey.toHexString(),
        )
        return try {
            val response = tangemTechApi.createUserNetworkAccount(
                body = request,
            ).getOrThrow()
            Result.Success(response.data.accountId)
        } catch (e: Exception) {
            Result.Failure(BlockchainSdkError.FailedToCreateAccount)
        }
    }
}