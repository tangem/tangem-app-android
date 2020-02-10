package com.tangem.blockchain.cardano

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.builder.ArrayBuilder
import com.tangem.blockchain.cardano.crypto.Blake2b
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.extensions.decodeBase58
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

class CardanoTransactionBuilder() {
    var unspentOutputs: List<UnspentOutput> = listOf()
    var transactionBody: ByteArray = ByteArray(0)

    fun buildToSign(transactionData: TransactionData): ByteArray {
        val transactionBuilder = CborBuilder()

        val transactionArray = transactionBuilder.addArray()
        transactionArray.addInputArray()
        transactionArray.addOutputArray(transactionData)
        transactionArray.addMap().end()

        val transactionBaos = ByteArrayOutputStream()
        CborEncoder(transactionBaos).encode(transactionBuilder.build())
        transactionBody = transactionBaos.toByteArray()

        val blake2b = Blake2b.Digest.newInstance(32)
        val transactionBodyHash = blake2b.digest(transactionBody)

        val magicBaos = ByteArrayOutputStream()
        CborEncoder(magicBaos).encode(CborBuilder().add(PROTOCOL_MAGIC).build())
        val magic = magicBaos.toByteArray()

        //dataToSign prefix
        val prefixedHashBaos = ByteArrayOutputStream()
        prefixedHashBaos.write(byteArrayOf(0x01.toByte()))
        prefixedHashBaos.write(magic)
        prefixedHashBaos.write(byteArrayOf(0x58.toByte(), 0x20.toByte()))
        prefixedHashBaos.write(transactionBodyHash)
        return prefixedHashBaos.toByteArray()
    }

    fun buildToSend(signature: ByteArray, publicKey: ByteArray): ByteArray {
        val extendedPublicKey = CardanoAddressFactory.extendPublicKey(publicKey)

        //pubkey + signature
        val witnessBodyBaos = ByteArrayOutputStream()
        CborEncoder(witnessBodyBaos).encode(CborBuilder()
                .addArray()
                    .add(extendedPublicKey)
                    .add(signature)
                .end()
                .build())
        val witnessBody = witnessBodyBaos.toByteArray()
        val witnessBodyItem = CborBuilder().add(witnessBody).build()[0]
        witnessBodyItem.setTag(24)

        val witnessBuilder = CborBuilder()
        val witnessArrayBuilder = witnessBuilder.addArray()

        //witness type + witness body
        for (utxo in unspentOutputs) {
            witnessArrayBuilder
                    .addArray()
                        .add(0)
                        .add(witnessBodyItem)
                    .end()
        }

        val witnessBaos = ByteArrayOutputStream()
        CborEncoder(witnessBaos).encode(witnessBuilder.build())
        val witness = witnessBaos.toByteArray()

        val transactionBaos = ByteArrayOutputStream()
        transactionBaos.write(byteArrayOf(0x82.toByte()))
        transactionBaos.write(transactionBody)
        transactionBaos.write(witness)
        return transactionBaos.toByteArray()
    }

    private fun ArrayBuilder<CborBuilder>.addInputArray() {
        val inputArray = this.startArray()

        for (utxo in unspentOutputs) {
            val inputBaos = ByteArrayOutputStream()
            CborEncoder(inputBaos).encode(CborBuilder()
                    .addArray()
                        .add(utxo.hash)
                        .add(utxo.outputIndex)
                    .end()
                    .build())
            val input = inputBaos.toByteArray()

            val inputItem = CborBuilder().add(input).build().get(0)
            inputItem.setTag(24)
            //input type + input
            inputArray
                    .addArray()
                        .add(0)
                        .add(inputItem)
                    .end()
        }
        inputArray.end()
    }

    private fun ArrayBuilder<CborBuilder>.addOutputArray(transactionData: TransactionData) {
        val amount = transactionData.amount.value!!
                .movePointRight(transactionData.amount.decimals.toInt()).toLong()
        val fee = transactionData.fee!!.value!!
                .movePointRight(transactionData.fee.decimals.toInt()).toLong()
        val change = calculateChange(amount, fee)

        val outputArray = this.startArray()

        //1st output
        val targetAddressItem =
                CborDecoder(ByteArrayInputStream(transactionData.destinationAddress.decodeBase58()))
                        .decode()[0]
        outputArray
                .addArray()
                    .add(targetAddressItem)
                    .add(amount)
                .end()

        //2nd output (optional)
        if (change > 0) {
            val myAddressItem =
                    CborDecoder(ByteArrayInputStream(transactionData.sourceAddress.decodeBase58()))
                            .decode()[0]
            outputArray
                    .addArray()
                        .add(myAddressItem)
                        .add(change)
                    .end()
        }
        outputArray.end()
    }

    private fun calculateChange(amount: Long, fee: Long): Long {
        val fullAmount = unspentOutputs.map { it.amount }.sum()
        return fullAmount - (amount + fee)
    }

    fun getEstimateSize(transactionData: TransactionData, walletPublicKey: ByteArray): Int {
        val dummyFeeValue = BigDecimal.valueOf(0.1)

        val dummyFee = transactionData.amount.copy(value = dummyFeeValue)
        val dummyAmount =
                transactionData.amount.copy(value = transactionData.amount.value!! - dummyFeeValue)

        val dummyTransactionData = transactionData.copy(
                amount = dummyAmount,
                fee = dummyFee
        )
        buildToSign(dummyTransactionData)
        return buildToSend(ByteArray(64), walletPublicKey).size
    }

    companion object {
        private const val PROTOCOL_MAGIC: Long = 764824073
    }
}

class UnspentOutput(
        val amount: Long,
        val outputIndex: Long,
        val hash: ByteArray
)