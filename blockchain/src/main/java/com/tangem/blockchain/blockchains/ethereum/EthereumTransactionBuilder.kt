package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.hexToBytes
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toFixedLengthByteArray
import org.kethereum.extensions.transactions.encodeRLP
import org.kethereum.extensions.transactions.tokenTransferSignature
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.*
import java.math.BigDecimal
import java.math.BigInteger

class EthereumTransactionBuilder(private val walletPublicKey: ByteArray, blockchain: Blockchain) {

    private val chainId = when (blockchain) {
        Blockchain.Ethereum -> Chain.Mainnet.id
        Blockchain.RSK -> Chain.RskMainnet.id
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by EthereumTransactionBuilder")
    }

    fun buildToSign(transactionData: TransactionData, nonce: BigInteger?): TransactionToSign? {

        val amount: BigDecimal = transactionData.amount.value ?: return null
        val transactionFee: BigDecimal = transactionData.fee?.value ?: return null

        val fee = transactionFee.movePointRight(transactionData.fee.decimals).toBigInteger()
        val gasLimit = getGasLimit(transactionData.amount).value.toBigInteger()
        val bigIntegerAmount = amount.movePointRight(transactionData.amount.decimals).toBigInteger()

        val to: Address
        val value: BigInteger
        val input: ByteArray //data for smart contract

        if (transactionData.amount.type == AmountType.Coin) { //coin transfer
            to = Address(transactionData.destinationAddress)
            value = bigIntegerAmount
            input = ByteArray(0)
        } else { //token transfer
            to = Address(transactionData.contractAddress
                    ?: throw Exception("Contract address is not specified!"))
            value = BigInteger.ZERO
            input = createErc20TransferData(transactionData.destinationAddress, bigIntegerAmount)
        }

        val transaction = createTransactionWithDefaults(
                from = Address(transactionData.sourceAddress),
                to = to,
                value = value,
                gasPrice = fee.divide(gasLimit),
                gasLimit = gasLimit,
                nonce = nonce,
                input = input
//                chain = ChainId(chainId.toLong())
        )
        val hash = transaction.encodeRLP(SignatureData(v = chainId.toBigInteger())).keccak()
        return TransactionToSign(transaction, listOf(hash))
    }

    fun buildToSend(signature: ByteArray, transactionToSign: TransactionToSign): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(transactionToSign.hashes[0], PublicKey(walletPublicKey.sliceArray(1..64)))
        val v = (recId + 27 + 8 + (chainId * 2)).toBigInteger()
        val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, v)

        return transactionToSign.transaction.encodeRLP(signatureData)
    }

    private fun createErc20TransferData(recepient: String, amount: BigInteger): ByteArray {
        return tokenTransferSignature.toByteArray() +
                recepient.substring(2).hexToBytes().toFixedLengthByteArray(32) +
                amount.toBytesPadded(32)
    }
}

class TransactionToSign(val transaction: Transaction, val hashes: List<ByteArray>)

enum class GasLimit(val value: Long) {
    Default(21000),
    Token(60000),
    High(300000)
}

internal fun getGasLimit(amount: Amount): GasLimit {
    return when (amount.currencySymbol) {
        Blockchain.Ethereum.currency -> GasLimit.Default
        "DGX" -> GasLimit.High
        "CGT" -> GasLimit.High
        else -> GasLimit.Token
    }
}

enum class Chain(val id: Int) {
    Mainnet(1),
    Morden(2),
    Ropsten(3),
    Rinkeby(4),
    RskMainnet(30),
    RskTestnet(31),
    Kovan(42),
    EthereumClassicMainnet(61),
    EthereumClassicTestnet(62),
    Geth_private_chains(1337),
    MaticTestnet(8995);
}