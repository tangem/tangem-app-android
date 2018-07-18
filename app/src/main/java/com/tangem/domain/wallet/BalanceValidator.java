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
        if(score < 0) score = 0;
        if (score ==0) {
            return "Do not accept. " + secondLine;
        } else {
            return score + "% safe. " + secondLine;
        }
    }
    public int GetColor() {
        if (score>89) { return R.color.msg_okay;}
        else if (score>74) { return android.R.color.holo_orange_light;}
        else if (score>0) { return android.R.color.holo_orange_dark;}
        else { return android.R.color.holo_red_light;}
    }

    public void Check(TangemCard card)
    {
        firstLine = "Verification failed";
        secondLine = "";

        // rule 1
        if(!CheckOfflineBalance(card) && !CheckOnlineBalance(card)) {
            score = 0;
            firstLine = "Unknown balance";
            secondLine = "Balance cannot be verified. Swipe down to refresh.";
            return;
        }
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

        // rule 3
        if(CheckOnlineBalance(card) && !NotConfirmTransaction(card))
        {
            if(card.isBalanceRecieved() && card.isBalanceEqual()) {
                score = 100;
                firstLine = "Verified balance";
                secondLine += "Confirmed balance and banknote identity. ";
                if(card.getBalance() == 0) {
                    firstLine = "Zero balance";
                }
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

        // rule 7
        if(CheckOfflineBalance(card) && !CheckOnlineBalance(card))
        {
            score = 90;
            firstLine = "Verified offline balance";
            secondLine += "Restore internet connection to be more confident. ";
            if(score <= 0)
                return;
        }

        // rule 2.b
        if(!CheckAttestationServiceAvailable(card))
        {
            score -= 15;
            secondLine += "Card identity was not verified. Cannot reach attestation service. ";
            if(score <= 0)
                return;
        }

        // rule 5
        if(IsLostSecondRead(card))
        {
            score -= 30;
            secondLine += "Wallet and banknote keys were not verified. Tap again. ";
            if(score <= 0)
                return;
        }

        // rule 4
        if(!CheckSignHashes(card))
        {
            score -= 50;
            firstLine = "Unguaranteed balance";
            secondLine += "Potential unsent transaction at the moment. Check this banknote later. ";
            if(score <= 0)
                return;
        }

        // rule 6
        if(NotConfirmTransaction(card))
        {
            score -= 50;
            String firstLine = "Unguaranted balance";
            secondLine += " Loading in progress. Wait for full confirmation in blockchain. ";
            if(score <= 0)
                return;
        }

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

    boolean CheckSignHashes(TangemCard card)
    {
        return card.getSignHashes()==null;
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

    boolean IsLostSecondRead(TangemCard card)
    {
        return card.isCodeConfirmed() != null;
    }
}
