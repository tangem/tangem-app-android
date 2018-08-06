package com.tangem.domain.wallet;

import com.tangem.wallet.R;

public class BalanceValidator {
    private String firstLine;
    private String secondLine;
    private int score;
    public String GetFirstLine()
    {
        return firstLine;
    }
    public String GetSecondLine()
    {
        if (score>89) { return "Safe to accept. " + secondLine;}
        else if (score>74) { return "Not fully safe to accept. " + secondLine;}
        else if (score>30) { return "Not safe to accept. " + secondLine;}
        else { return "Do not accept! " + secondLine;}
    }
    public int GetColor() {
        if (score>89) { return R.color.confirmed;}
        else if (score>74) { return android.R.color.holo_orange_light;}
        else if (score>0) { return android.R.color.holo_orange_dark;}
        else { return android.R.color.holo_red_light;}
    }

    public void Check(TangemCard card)
    {


        firstLine = "Verification failed";
        secondLine = "";

        // rule 1
        if((!CheckOfflineBalance(card) && !CheckOnlineBalance(card)) || (!CheckOnlineBalance(card) && HasEverSigned(card))) {
            score = 0;
            firstLine = "Unknown balance";
            secondLine = "Balance cannot be verified. Swipe down to refresh.";
            return;
        }

        // Workaround before new back-end
//        if (!HasEverSigned(card)) {
//            firstLine = "Verified balance";
//            secondLine = "Balance confirmed in blockchain. ";
//            secondLine += "Verified note identity. ";
//            return;
//        }

        // rule 2.a
        if(!VerificationWalletKey(card)) {
            score = 0;
            firstLine = "Verification failed";
            secondLine = "Wallet verification failed. Tap again.";
            return;
        }

        // rule 2.c
        if(!CheckAttestationServiceResult(card) && CheckAttestationServiceAvailable(card)) {
            score = 0;
            firstLine = "Not genuine banknote";
            secondLine = "Tangem Attestation service says the banknote is not genuine.";
            return;
        }

        // rule 6
        if(NotConfirmTransaction(card))
        {
            score = 0;
            firstLine = "Unguaranted balance";
            secondLine = "Loading in progress. Wait for full confirmation in blockchain. ";
            return;
        }

        // rule 5
        if(!isCodeConfirmed(card))
        {
            score = 0;
            firstLine = "Not genuine banknote";
            secondLine = "Firmware binary code verification failed";
            return;
        }

        // rule 5
        if(card.PIN2 == TangemCard.PIN2_Mode.CustomPIN2)
        {
            score = 0;
            firstLine = "Locked with PIN2";
            secondLine = "Ask the holder to disable PIN2 before accepting";
            return;
        }

        if(card.isBalanceRecieved() && card.isBalanceEqual()) {
            score = 100;
            firstLine = "Verified balance";
            secondLine = "Balance confirmed in blockchain. ";
            if(card.getBalance() == 0) {
                firstLine = "";
            }
        }

        // rule 4 TODO: need to check SignedHashed against number of outputs in blockchain
//        if(HasEverSigned(card) && card.getBalance() != 0)
//        {
//            score = 80;
//            firstLine = "Unguaranteed balance";
//            secondLine = "Potential unsent transaction. Redeem immediately if accept. ";
//            return;
//        }

        // rule 7
        if(CheckOfflineBalance(card) && !CheckOnlineBalance(card) && !HasEverSigned(card) && card.getBalance() != 0)
        {
            score = 80;
            firstLine = "Verified offline balance";
            secondLine = "Can't obtain balance from blockchain. Restore internet connection to be more confident. ";
        }

        // rule 2.b
        if(CheckAttestationServiceAvailable(card))
        {
            secondLine += "Verified note identity. ";
        } else {
            score = 80;
            secondLine += "Card identity was not verified. Cannot reach Tangem attestation service. ";
        }

//            if(card.getFailedBalanceRequestCounter()!=0) {
//                score -= 5 * card.getFailedBalanceRequestCounter();
//                secondLine += "Not all nodes have returned balance. Swipe down or tap again. ";
//                if(score <= 0)
//                    return;
//            }

        //
//            if(card.isBalanceRecieved() && !card.isBalanceEqual()) {
//                score = 0;
//                firstLine = "Disputed balance";
//                secondLine += " Cannot obtain trusted balance at the moment. Try to tap and check this banknote later.";
//                return;
//            }


    }

    boolean CheckOfflineBalance(TangemCard card)
    {
        return card.getOfflineBalance() != null;
    }

    boolean CheckOnlineBalance(TangemCard card)
    {
        return card.isBalanceRecieved();
    }

    boolean VerificationWalletKey(TangemCard card)
    {
        return card.isWalletPublicKeyValid();
    }

    boolean HasEverSigned(TangemCard card)
    {
//        return card.getSignedHashes() != 0 || card.getRemainingSignatures() != card.getMaxSignatures();
        return card.getRemainingSignatures() != card.getMaxSignatures();
    }

    boolean CheckAttestationServiceAvailable(TangemCard card)
    {
        return card.isOnlineVerified() != null;
    }

    boolean CheckAttestationServiceResult(TangemCard card)
    {
        return card.isOnlineVerified() != null && card.isOnlineVerified() == true;
    }

    boolean NotConfirmTransaction(TangemCard card)
    {
        return card.getBalanceUnconfirmed()!=0;
    }

    boolean isCodeConfirmed(TangemCard card)
    {
        return card.isCodeConfirmed() == null || card.isCodeConfirmed();
    }
}
