//package com.tangem.tap.domain.tasks
//
//import com.tangem.common.CompletionResult
//import com.tangem.common.core.CardSession
//import com.tangem.common.core.CardSessionRunnable
//import com.tangem.common.core.CompletionCallback
//import com.tangem.common.hdWallet.DerivationPath
//import com.tangem.common.hdWallet.ExtendedPublicKey
//import com.tangem.operations.CommandResponse
//import com.tangem.operations.derivation.DeriveWalletPublicKeysTask
//import com.tangem.tap.common.extensions.ByteArrayKey
//
//class ExtendedPublicKeyList(
//    items: Collection<ExtendedPublicKey>
//): ArrayList<ExtendedPublicKey>(items), CommandResponse
//
//
//class DerivationTaskResponse(
//    val entries: Map<ByteArrayKey, ExtendedPublicKeyList>
//): CommandResponse
//
//class DerivationTask(
//    private val derivations: Map<ByteArrayKey, List<DerivationPath>>
//) : CardSessionRunnable<DerivationTaskResponse> {
//
//    val response: MutableMap<ByteArrayKey, ExtendedPublicKeyList> = mutableMapOf()
//
//    override fun run(session: CardSession, callback: CompletionCallback<DerivationTaskResponse>) {
//        derive(keys = derivations.keys.toList(), index = 0, session = session, callback = callback)
//    }
//
//    private fun derive(
//        keys: List<ByteArrayKey>,
//        index: Int,
//        session: CardSession,
//        callback: CompletionCallback<DerivationTaskResponse>
//    ) {
//        if (index == keys.count()) {
//            callback(CompletionResult.Success(DerivationTaskResponse(response.toMap())))
//            return
//        }
//
//        val key = keys[index]
//        val paths = derivations[key]!!
//        DeriveWalletPublicKeysTask(key.bytes, paths).run(session) { result ->
//            when (result) {
//                is CompletionResult.Success -> {
//                    response[key] = result.data[key]
//                    derive(keys = keys, index = index + 1, session = session, callback = callback)
//                }
//                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
//            }
//        }
//    }
//}