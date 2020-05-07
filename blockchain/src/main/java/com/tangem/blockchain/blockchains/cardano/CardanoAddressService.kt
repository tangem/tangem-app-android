package com.tangem.blockchain.blockchains.cardano

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.UnsignedInteger
import com.tangem.blockchain.blockchains.cardano.crypto.Blake2b
import com.tangem.blockchain.common.AddressService
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase58
import org.spongycastle.crypto.util.DigestFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class CardanoAddressService : AddressService {
    override fun makeAddress(walletPublicKey: ByteArray): String {
        val extendedPublicKey = extendPublicKey(walletPublicKey)

        val pubKeyWithAttributes = getPubKeyWithAttributes(extendedPublicKey)
        val sha3Hash = getSha3Hash(pubKeyWithAttributes)

        val blake2b = Blake2b.Digest.newInstance(28)
        val blakeHash = blake2b.digest(sha3Hash)

        val hashWithAttributes = getHashWithAttributes(blakeHash)

        return getAddress(hashWithAttributes)
    }

    override fun validate(address: String): Boolean {
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

    private fun getPubKeyWithAttributes(extendedPublicKey: ByteArray): ByteArray {
        val pubKeyWithAttributes = ByteArrayOutputStream()
        CborEncoder(pubKeyWithAttributes).encode(CborBuilder()
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
        return pubKeyWithAttributes.toByteArray()
    }

    private fun getSha3Hash(pubKeyWithAttributes: ByteArray): ByteArray {
        val sha3Digest = DigestFactory.createSHA3_256()
        sha3Digest.update(pubKeyWithAttributes, 0, pubKeyWithAttributes.size)
        val sha3Hash = ByteArray(32)
        sha3Digest.doFinal(sha3Hash, 0)
        return sha3Hash
    }

    private fun getHashWithAttributes(blakeHash: ByteArray): ByteArray {
        val hashWithAttributes = ByteArrayOutputStream()
        CborEncoder(hashWithAttributes).encode(CborBuilder()
                .addArray()
                    .add(blakeHash)
                    .addMap() //additional attributes
                    .end()
                    .add(0) //address type
                .end()
                .build())
        return hashWithAttributes.toByteArray()
    }

    private fun getAddress(hashWithAttributes: ByteArray): String {
        val checksum = getCheckSum(hashWithAttributes)

        val addressItem = CborBuilder().add(hashWithAttributes).build().get(0)
        addressItem.setTag(24)

        //addr + checksum
        val address = ByteArrayOutputStream()
        CborEncoder(address).encode(CborBuilder()
                .addArray()
                    .add(addressItem)
                    .add(checksum)
                .end()
                .build())

        return address.toByteArray().encodeBase58()
    }

    private fun getCheckSum(hashWithAttributes: ByteArray): Long {
        val crc32 = CRC32()
        crc32.update(hashWithAttributes)
        return crc32.value
    }

    companion object {
        fun extendPublicKey(publicKey: ByteArray): ByteArray {
            val zeroBytes = ByteArray(32)
            zeroBytes.fill(0)
            return publicKey + zeroBytes
        }
    }
}