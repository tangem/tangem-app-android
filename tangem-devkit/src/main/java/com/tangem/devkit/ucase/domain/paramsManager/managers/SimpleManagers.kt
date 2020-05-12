package com.tangem.devkit.ucase.domain.paramsManager.managers

import com.tangem.devkit._arch.structure.impl.EditTextItem
import com.tangem.devkit.ucase.domain.actions.*
import com.tangem.devkit.ucase.domain.paramsManager.triggers.changeConsequence.CardIdConsequence
import com.tangem.devkit.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 */
class ScanItemsManager : BaseItemsManager(ScanAction())

class DepersonalizeItemsManager : BaseItemsManager(DepersonalizeAction()) {

    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(EditTextItem(TlvId.CardId, null)))
    }
}

class SignItemsManager : BaseItemsManager(SignAction()) {

    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(
                EditTextItem(TlvId.CardId, null),
                EditTextItem(TlvId.TransactionOutHash, "Data used for hashing")
        ))
    }
}

class CreateWalletItemsManager : BaseItemsManager(CreateWalletAction()) {

    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(EditTextItem(TlvId.CardId, null)))
    }
}

class PurgeWalletItemsManager : BaseItemsManager(PurgeWalletAction()) {

    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(EditTextItem(TlvId.CardId, null)))
    }
}

class ReadIssuerDataItemsManager : BaseItemsManager(ReadIssuerDataAction()) {

    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(EditTextItem(TlvId.CardId, null)))
    }
}

class WriteIssuerDataItemsManager : BaseItemsManager(WriteIssuerDataAction()) {
    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(
                EditTextItem(TlvId.CardId, null),
                EditTextItem(TlvId.IssuerData, "Data to be written on a card as issuer data"),
                EditTextItem(TlvId.Counter, "1")
        ))
    }
}

class ReadIssuerExtraDataItemsManager : BaseItemsManager(ReadIssuerExtraDataAction()) {

    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(EditTextItem(TlvId.CardId, null)))
    }
}

class WriteIssuerExtraDataItemsManager : BaseItemsManager(WriteIssuerExtraDataAction()) {
    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(
                EditTextItem(TlvId.CardId, null),
                EditTextItem(TlvId.Counter, "1")
        ))
    }
}

class ReadUserDataItemsManager : BaseItemsManager(ReadUserDataAction()) {

    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(EditTextItem(TlvId.CardId, null)))
    }
}

class WriteUserDataItemsManager : BaseItemsManager(WriteUserDataAction()) {
    init {
        setItemChangeConsequences(CardIdConsequence())
        setItems(listOf(
                EditTextItem(TlvId.CardId, null),
                EditTextItem(TlvId.Counter, "1"),
                EditTextItem(TlvId.UserData, "User data to be written on a card")
        ))
    }
}

    class WriteProtectedUserDataItemsManager : BaseItemsManager(WriteUserProtectedDataAction()) {
        init {
            setItemChangeConsequences(CardIdConsequence())
            setItems(listOf(
                    EditTextItem(TlvId.CardId, null),
                    EditTextItem(TlvId.Counter, "1"),
                    EditTextItem(TlvId.ProtectedUserData, "Protected user data to be written on a card")
            ))
        }
}