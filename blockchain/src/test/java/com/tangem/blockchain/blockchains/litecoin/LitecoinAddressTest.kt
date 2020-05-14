package com.tangem.blockchain.blockchains.litecoin

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class LitecoinAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Litecoin)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "044A76C9A70422160F515F956D0F50C71BBBA4F9862A22913817D63F0B1EF7C2FAF512E1C91B1BE827560EFE24FB1652B47337E296C778DFB1014D080CDD35EF65".hexToBytes()
        val expected = "LeweDi2SMmishGyCqQN2972qNByaSRdcfT"

        Truth.assertThat(addressService.makeAddress(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "LeweDi2SMmishGyCqQN2972qNByaSRdcfT"
        Truth.assertThat(addressService.validate(address))
                .isTrue()
    }
}