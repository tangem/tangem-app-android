package com.tangem.blockchain.blockchains.bitcoin


import com.tangem.blockchain.blockchains.litecoin.LitecoinMainNetParams
import com.tangem.blockchain.common.AddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import java.security.MessageDigest

class BitcoinAddressService(private val blockchain: Blockchain) : AddressService {

    private val networkParameters: NetworkParameters = when (blockchain) {
        Blockchain.Bitcoin -> MainNetParams()
        Blockchain.BitcoinTestnet -> TestNet3Params()
        Blockchain.Litecoin -> LitecoinMainNetParams()
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    override fun makeAddress(walletPublicKey: ByteArray): String {
        val publicKeyHash = walletPublicKey.calculateSha256().calculateRipemd160()
        val checksum = byteArrayOf(networkParameters.addressHeader.toByte()).plus(publicKeyHash)
                .calculateSha256().calculateSha256()
        val result = byteArrayOf(networkParameters.addressHeader.toByte()) + publicKeyHash + checksum.copyOfRange(0, 4)
        return Base58.encode(result)
    }

    override fun validate(address: String): Boolean {
        return validateLegacyAddress(address) || validateSegwitAddress(address)
    }

    private fun validateSegwitAddress(address: String): Boolean {
        return try {
            when (blockchain) {
                Blockchain.Bitcoin -> SegwitAddress.fromBech32(MainNetParams(), address)
                Blockchain.BitcoinTestnet -> SegwitAddress.fromBech32(TestNet3Params(), address)
                Blockchain.Litecoin -> SegwitAddress.fromBech32(LitecoinMainNetParams(), address)
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun validateLegacyAddress(address: String): Boolean {
        return try {
            when (blockchain) {
                Blockchain.Bitcoin -> LegacyAddress.fromBase58(MainNetParams(), address)
                Blockchain.BitcoinTestnet -> LegacyAddress.fromBase58(TestNet3Params(), address)
                Blockchain.Litecoin -> LegacyAddress.fromBase58(LitecoinMainNetParams(), address)
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}