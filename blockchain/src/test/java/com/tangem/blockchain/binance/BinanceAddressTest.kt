package com.tangem.blockchain.binance

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class BinanceAddressTest {
    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A545618C174251B984DF841F49D2376F".hexToBytes()
        val expected = "bnb1m9wu5ncxk8mqhm3nfmxa552728wkty700rzhvu"

        Truth.assertThat(BinanceAddressFactory.makeAddress(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "bnb1m9wu5ncxk8mqhm3nfmxa552728wkty700rzhvu"
        Truth.assertThat(BinanceAddressValidator.validate(address))
                .isTrue()
    }
}