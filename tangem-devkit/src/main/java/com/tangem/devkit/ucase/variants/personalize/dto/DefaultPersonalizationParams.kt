package com.tangem.devkit.ucase.variants.personalize.dto

import com.tangem.KeyPair
import com.tangem.commands.personalization.entities.Acquirer
import com.tangem.commands.personalization.entities.Issuer
import com.tangem.commands.personalization.entities.Manufacturer
import com.tangem.common.extensions.hexToBytes

/**
[REDACTED_AUTHOR]
 */

interface DefaultPersonalizationParams {
    companion object {

        fun issuer(): Issuer {
            val name = "TANGEM SDK"
            return Issuer(
                    name = name,
                    id = name + "\u0000",
                    dataKeyPair = KeyPair(
                            ("045f16bd1d2eafe463e62a335a09e6b2bbcbd04452526885cb679fc4d27af1bd22f553c7deefb54fd3d4f" +
                                    "361d14e6dc3f11b7d4ea183250a60720ebdf9e110cd26").hexToBytes(),
                            "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()
                    ),
                    transactionKeyPair = KeyPair(
                            ("0484c5192e9bfa6c528a344f442137a92b89ea835bfef1d04cb4362eb906b508c5889846cfea71ba6dc7b" +
                                    "3120c2208df9c46127d3d85cb5cfbd1479e97133a39d8").hexToBytes(),
                            "11121314151617184771ED81F2BACF57479E4735EB1405081918171615141312".hexToBytes()
                    )
            )
        }

        fun acquirer(): Acquirer {
            val name = "Smart Cash"
            return Acquirer(
                    name = name,
                    id = name + "\u0000",
                    keyPair = KeyPair(
                            ("0456ad1a82b22bcb40c38fd08939f87e6b80e40dec5b3bdb351c55fcd709e47f9fb2ed00c2304d3a9" +
                                    "86f79c5ae0ac3c84e88da46dc8f513b7542c716af8c9a2daf").hexToBytes(),
                            "21222324252627284771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()
                    )
            )
        }

        fun manufacturer(): Manufacturer {
            val keys = Manufacturer(com.tangem.devkit.ucase.variants.personalize.dto.Manufacturer.Mode.Developer)
            return Manufacturer(
                    name = "Tangem",
                    keyPair = KeyPair(keys.publicKey, keys.privateKey)
            )
        }
    }
}