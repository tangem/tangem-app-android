package com.tangem.blockchain.blockchains.ducatus

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class DucatusAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Ducatus)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "0485D520C8B907F0BC5E03FCBBAC212CCD270764BBFF4990A28653A2FB0D656C342DF143C4D52C43582289E20A81D5D014C1384A1FFFEA1D121903AD7ED35A01EA".hexToBytes()
        val expected = "Ly3SZetcgr5gkZMwiNwVrts2z2r3jYieAG"

        Truth.assertThat(addressService.makeAddress(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "Ly3SZetcgr5gkZMwiNwVrts2z2r3jYieAG"
        Truth.assertThat(addressService.validate(address))
                .isTrue()
    }
}