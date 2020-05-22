package com.tangem.blockchain.blockchains.tezos

import com.tangem.blockchain.common.AddressService
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.Base58
import org.spongycastle.jcajce.provider.digest.Blake2b

class TezosAddressService : AddressService {
    override fun makeAddress(walletPublicKey: ByteArray): String {
        val publicKeyHash = Blake2b.Blake2b160().digest(walletPublicKey)

        val tz1Prefix = "06A19F".hexToBytes()
        val prefixedHash = tz1Prefix + publicKeyHash

        val checksum = prefixedHash.calculateTezosChecksum()
        val prefixedHashWithChecksum = prefixedHash + checksum

        return Base58.encode(prefixedHashWithChecksum)
    }

    override fun validate(address: String): Boolean {
        val prefixedHashWithChecksum = Base58.decode(address)
        if (prefixedHashWithChecksum == null || prefixedHashWithChecksum.size != 27) return false

        val prefixedHash = prefixedHashWithChecksum.copyOf(23)
        val checksum = prefixedHashWithChecksum.copyOfRange(23, 27)

        val calculatedChecksum = prefixedHash.calculateTezosChecksum()

        return calculatedChecksum.contentEquals(checksum)
    }

    companion object {
        fun ByteArray.calculateTezosChecksum() = this.calculateSha256().calculateSha256().copyOfRange(0, 4)
    }
}