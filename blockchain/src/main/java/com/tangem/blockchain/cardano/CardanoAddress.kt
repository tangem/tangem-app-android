package com.tangem.blockchain.cardano

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.UnsignedInteger
import com.tangem.blockchain.cardano.crypto.Blake2b
import com.tangem.blockchain.common.extensions.decodeBase58
import com.tangem.blockchain.common.extensions.encodeBase58
import org.spongycastle.crypto.util.DigestFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class CardanoAddressFactory {
    companion object {
        fun makeAddress(cardPublicKey: ByteArray, testNet: Boolean = false): String {
            val extendedPublicKey = extendPublicKey(cardPublicKey)

            val pubKeyWithAttributesBaos = ByteArrayOutputStream()
            CborEncoder(pubKeyWithAttributesBaos).encode(CborBuilder()
                    .addArray()
                        .add(0)
                            .addArray()
                                .add(0)
                                .add(extendedPublicKey)
                            .end()
                        .addMap()
                        .end()
                    .end()
                    .build())
            val pubKeyWithAttributes = pubKeyWithAttributesBaos.toByteArray()

            val sha3Digest = DigestFactory.createSHA3_256()
            sha3Digest.update(pubKeyWithAttributes, 0, pubKeyWithAttributes.size)
            val sha3Hash = ByteArray(32)
            sha3Digest.doFinal(sha3Hash, 0)

            val blake2b = Blake2b.Digest.newInstance(28)
            val blakeHash = blake2b.digest(sha3Hash)

            val hashWithAttributesBaos = ByteArrayOutputStream()
            CborEncoder(hashWithAttributesBaos).encode(CborBuilder()
                    .addArray()
                        .add(blakeHash)
                        .addMap() //additional attributes
                        .end()
                        .add(0) //address type
                    .end()
                    .build())
            val hashWithAttributes = hashWithAttributesBaos.toByteArray()

            val crc32 = CRC32()
            crc32.update(hashWithAttributes)
            val checksum = crc32.value

            val addressItem = CborBuilder().add(hashWithAttributes).build().get(0)
            addressItem.setTag(24)

            //addr + checksum
            val addressBaos = ByteArrayOutputStream()
            CborEncoder(addressBaos).encode(CborBuilder()
                    .addArray()
                        .add(addressItem)
                        .add(checksum)
                    .end()
                    .build())

            val hexAddress = addressBaos.toByteArray()
            return hexAddress.encodeBase58()
        }

        fun extendPublicKey(publicKey: ByteArray): ByteArray {
            val zeroBytes = ByteArray(32)
            zeroBytes.fill(0)
            return publicKey + zeroBytes
        }
    }
}

class CardanoAddressValidator {
    companion object {
        fun validate(address: String): Boolean {
            val decoded = address.decodeBase58() ?: return false

            return try {
                val bais = ByteArrayInputStream(decoded)
                val addressList =
                        (CborDecoder(bais).decode()[0] as Array).dataItems
                val addressItemBytes = (addressList[0] as ByteString).bytes
                val checksum = (addressList[1] as UnsignedInteger).value.toLong()

                val crc32 = CRC32()
                crc32.update(addressItemBytes)
                val calculatedChecksum = crc32.value

                checksum == calculatedChecksum
            } catch (e: Exception) {
                false
            }
        }
    }
}