package com.tangem.tangemtest.ucase.variants.personalize.dto

import com.tangem.KeyPair
import com.tangem.commands.personalization.entities.Acquirer
import com.tangem.commands.personalization.entities.Issuer
import com.tangem.commands.personalization.entities.Manufacturer

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
                            "".toByteArray(),
                            "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".toByteArray()
                    ),
                    transactionKeyPair = KeyPair(
                            "".toByteArray(),
                            "11121314151617184771ED81F2BACF57479E4735EB1405081918171615141312".toByteArray()
                    )
            )
        }

        fun acquirer(): Acquirer {
            val name = "Smart Cash"
            return Acquirer(
                    name = name,
                    id = name + "\u0000",
                    keyPair = KeyPair(
                            "".toByteArray(),
                            "21222324252627284771ED81F2BACF57479E4735EB1405083927372D40DA9E92".toByteArray()
                    )
            )
        }

        fun manufacturer(): Manufacturer {
            val keys = Manufacturer(com.tangem.tangemtest.ucase.variants.personalize.dto.Manufacturer.Mode.Developer)
            return Manufacturer(
                    name = "Tangem",
                    keyPair = KeyPair(keys.publicKey, keys.privateKey)
            )
        }
    }
}