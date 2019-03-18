package com.ripple.core.coretypes;

import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.generic.Validation;
import com.ripple.core.types.known.sle.LedgerHashes;
import com.ripple.core.types.known.sle.entries.*;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;
import com.ripple.core.types.known.tx.txns.*;
import com.ripple.core.types.known.tx.txns.pseudo.EnableAmendment;
import com.ripple.core.types.known.tx.txns.pseudo.SetFee;

public class STObjectFormatter {
    public static STObject format(STObject source) {
        // This would need to go before the test that just checks
        // for ledgerEntryType
        if (AffectedNode.isAffectedNode(source)) {
            return new AffectedNode(source);
        }

        if (TransactionMeta.isTransactionMeta(source)) {
            TransactionMeta meta = new TransactionMeta();
            meta.fields = source.fields;
            return meta;
        }

        LedgerEntryType ledgerEntryType = STObject.ledgerEntryType(source);
        if (ledgerEntryType != null) {
            return ledgerFormatted(source, ledgerEntryType);
        }

        TransactionType transactionType = STObject.transactionType(source);
        if (transactionType != null) {
            return transactionFormatted(source, transactionType);
        }

        if (Validation.isValidation(source)) {
            Validation validation = new Validation();
            validation.fields = source.fields;
            return validation;
        }

        return source;
    }

    private static STObject transactionFormatted(STObject source, TransactionType transactionType) {
        STObject constructed = null;
        switch (transactionType) {
            case Payment:
                constructed = new Payment();
                break;
            case EscrowCreate:
                constructed = new EscrowCreate();
                break;
            case EscrowFinish:
                constructed = new EscrowFinish();
                break;
            case AccountSet:
                constructed = new AccountSet();
                break;
            case EscrowCancel:
                constructed = new EscrowCancel();
                break;
            case SetRegularKey:
                constructed = new SetRegularKey();
                break;
            case OfferCreate:
                constructed = new OfferCreate();
                break;
            case OfferCancel:
                constructed = new OfferCancel();
                break;
            case TicketCreate:
                constructed = new TicketCreate();
                break;
            case TicketCancel:
                constructed = new TicketCancel();
                break;
            case SignerListSet:
                constructed = new SignerListSet();
                break;
            case PaymentChannelCreate:
                constructed = new PaymentChannelCreate();
                break;
            case PaymentChannelFund:
                constructed = new PaymentChannelFund();
                break;
            case PaymentChannelClaim:
                constructed = new PaymentChannelClaim();
                break;
            case CheckCreate:
                constructed = new CheckCreate();
                break;
            case CheckCash:
                constructed = new CheckCash();
                break;
            case CheckCancel:
                constructed = new CheckCancel();
                break;
            case DepositPreauth:
                constructed = new DepositPreauth();
                break;
            case TrustSet:
                constructed = new TrustSet();
                break;
            case EnableAmendment:
                constructed = new EnableAmendment();
                break;
            case SetFee:
                constructed = new SetFee();
                break;
        }

        constructed.fields = source.fields;
        return constructed;

    }

    private static STObject ledgerFormatted(STObject source, LedgerEntryType ledgerEntryType) {
        STObject constructed = null;
        switch (ledgerEntryType) {
            case Escrow:
                constructed = new Escrow();
                break;
            case Offer:
                constructed = new Offer();
                break;
            case RippleState:
                constructed = new RippleState();
                break;
            case AccountRoot:
                constructed = new AccountRoot();
                break;
            case DirectoryNode:
                if (source.has(AccountID.Owner)) {
                    constructed = new OwnerDirectory();
                } else {
                    constructed = new OfferDirectory();
                }
                break;
            case LedgerHashes:
                constructed = new LedgerHashes();
                break;
            case Amendments:
                constructed = new Amendments();
                break;
            case FeeSettings:
                constructed = new FeeSettings();
                break;
            case Ticket:
                constructed = new Ticket();
                break;
            case SignerList:
                constructed = new SignerList();
                break;
            case PayChannel:
                constructed = new PayChannel();
                break;
            case Check:
                constructed = new Check();
                break;
            case DepositPreauth:
                constructed = new DepositPreauthLe();
                break;
        }
        constructed.fields = source.fields;
        return constructed;
    }
}
