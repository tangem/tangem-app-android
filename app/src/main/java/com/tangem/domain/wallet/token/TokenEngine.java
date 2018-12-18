package com.tangem.domain.wallet.token;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;

import com.google.common.base.Strings;
import com.tangem.data.network.ServerApiInfura;
import com.tangem.data.network.model.InfuraResponse;
import com.tangem.domain.wallet.BalanceValidator;
import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.ECDSASignatureETH;
import com.tangem.domain.wallet.EthTransaction;
import com.tangem.domain.wallet.Keccak256;
import com.tangem.domain.wallet.eth.EthData;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.domain.wallet.BTCUtils;
import com.tangem.tangemcard.tasks.SignTask;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.R;

import org.bitcoinj.core.ECKey;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

/**
 * Created by Ilia on 20.03.2018.
 */

public class TokenEngine extends CoinEngine {

    public TokenData coinData = null;

    public TokenEngine(TangemContext ctx) throws Exception {
        super(ctx);
        if (ctx.getCoinData() == null) {
            coinData = new TokenData();
            ctx.setCoinData(coinData);
        } else if (ctx.getCoinData() instanceof TokenData) {
            coinData = (TokenData) ctx.getCoinData();
        } else if (ctx.getCoinData() instanceof EthData) {
            // special case with receive card data substitution from server at the moment
            Bundle B=new Bundle();
            ctx.getCoinData().saveToBundle(B);
            coinData = new TokenData();
            coinData.loadFromBundle(B);
            ctx.setCoinData(coinData);
        } else {
            throw new Exception("Invalid type of Blockchain data for TokenEngine");
        }
    }

    public TokenEngine() {
        super();
    }


    @Override
    public boolean awaitingConfirmation() {
        return false;
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) {
            return null;
        }
        try {
            if (coinData.getBalanceInInternalUnits().notZero()) {
                return convertToAmount(coinData.getBalanceInInternalUnits());
            } else {
                return convertToAmount(coinData.getBalanceAlterInInternalUnits());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getBalanceHTML() {
        if (hasBalanceInfo()) {
            try {
                return " " + convertToAmount(coinData.getBalanceInInternalUnits()).toDescriptionString(getTokenDecimals()) + " <br><small><small>  + " + convertToAmount(coinData.getBalanceAlterInInternalUnits()).toDescriptionString(getEthDecimals()) + " for gas</small></small>";
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        String currency = ctx.getCard().getTokenSymbol();
        if (Strings.isNullOrEmpty(currency))
            return "NoN";
        if (hasBalanceInfo()) {
            if (coinData.getBalanceInInternalUnits().notZero()) {
                return currency;
            } else {
                return "ETH";
            }
        } else {
            return currency;
        }
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        if (!hasBalanceInfo()) return null;
        if (coinData.getBalanceInInternalUnits().notZero()) {
            return new InputFilter[]{new DecimalDigitsInputFilter(getTokenDecimals())};
        } else {
            return new InputFilter[]{new DecimalDigitsInputFilter(getEthDecimals())};
        }
    }

    @Override
    public String getFeeCurrency() {
        return "ETH";
    }

    @Override
    public String getOfflineBalanceHTML() {
        return ctx.getString(R.string.not_implemented);
    }

    public static int getEthDecimals() {
        return 18;
    }

    public int getTokenDecimals() {
        return ctx.getCard().getTokensDecimal();
    }

    public String getContractAddress(TangemCard card) {
        return card.getContractAddress();
    }

    public boolean isNeedCheckNode() {
        return false;
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (!address.startsWith("0x") && !address.startsWith("0X")) {
            return false;
        }

        if (address.length() != 42) {
            return false;
        }

        return true;
    }

    public boolean isBalanceAlterNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceAlterInInternalUnits() == null) return false;
        return coinData.getBalanceAlterInInternalUnits().notZero();
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null && coinData.getBalanceAlterInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero() || coinData.getBalanceAlterInInternalUnits().notZero();
    }


    @Override
    public String getBalanceEquivalent() {
        if (!hasBalanceInfo()) {
            return "";
        }
        try {
            if (coinData.getBalanceInInternalUnits().notZero()) {
                // TODO: check why Rate=EthRate
                return "";//convertToAmount(coinData.getBalanceInInternalUnits()).toEquivalentString(coinData.getRate());
            } else {
                if (coinData.getBalanceAlterInInternalUnits() == null) return "";
                return convertToAmount(coinData.getBalanceAlterInInternalUnits()).toEquivalentString(coinData.getRateAlter());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {
        Keccak256 kec = new Keccak256();
        int lenPk = pkUncompressed.length;
        if (lenPk < 2) {
            throw new IllegalArgumentException("Uncompress public key length is invald");
        }
        byte[] cleanKey = new byte[lenPk - 1];
        for (int i = 0; i < cleanKey.length; ++i) {
            cleanKey[i] = pkUncompressed[i + 1];
        }
        byte[] r = kec.digest(cleanKey);

        byte[] address = new byte[20];
        for (int i = 0; i < 20; ++i) {
            address[i] = r[i + 12];
        }

        return String.format("0x%s", BTCUtils.toHex(address));
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) throws Exception {
        if (internalAmount.getCurrency().equals("wei")) {
            BigDecimal d = internalAmount.divide(new BigDecimal("1000000000000000000"), getEthDecimals(), RoundingMode.DOWN);
            return new Amount(d, "ETH");
        } else if (internalAmount.getCurrency().equals(ctx.getCard().getTokenSymbol())) {
            BigDecimal p = new BigDecimal(10);
            p = p.pow(getTokenDecimals());
            BigDecimal d = internalAmount.divide(p);
            return new Amount(d, ctx.getCard().getTokenSymbol());
        }
        throw new Exception(String.format("Can't convert '%s' to '%s'", internalAmount.getCurrency(), ctx.getCard().getTokenSymbol()));
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) throws Exception {
        if (amount.getCurrency().equals("ETH")) {
            BigDecimal d = amount.multiply(new BigDecimal("1000000000000000000"));
            return new InternalAmount(d, "wei");
        } else if (amount.getCurrency().equals(ctx.getCard().getTokenSymbol())) {
            BigDecimal p = new BigDecimal(10);
            p = p.pow(getTokenDecimals());
            BigDecimal d = amount.multiply(p);
            return new InternalAmount(d, ctx.getCard().getTokenSymbol());
        }
        throw new Exception(String.format("Can't convert '%s' to '%s'", amount.getCurrency(), ctx.getCard().getTokenSymbol()));
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount amount) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public CoinData createCoinData() {
        return new TokenData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }

//    @Override
//    public String getFeeEquivalentDescriptor(String value) {
//        BigDecimal d = new BigDecimal(value);
//        return EthEngine.getAmountEquivalentDescription(d, coinData.getRateAlter());
//    }

    @Override
    public boolean hasBalanceInfo() {
        return coinData != null && coinData.getBalanceInInternalUnits() != null && coinData.getBalanceAlterInInternalUnits() != null;
    }


    @Override
    public Uri getShareWalletUriExplorer() {
        return Uri.parse("https://etherscan.io/token/" + getContractAddress(ctx.getCard()) + "?a=" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse("ethereum:" + ctx.getCoinData().getWallet());// + "?value=" + mCard.getDenomination() +"e18");
        } else {
            return Uri.parse("ethereum:" + ctx.getCoinData().getWallet());
        }
    }

    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.cannot_obtain_data_from_blockchain);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.please_wait_while_previous);
        } else if (!isBalanceAlterNotZero()) {
            ctx.setMessage(ctx.getString(R.string.not_enough_eth_for_gas));
        } else {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (!hasBalanceInfo()) return false;
        Amount balance;
        try {
            if (amount.getCurrency().equals(ctx.getCard().tokenSymbol)) {
                balance = convertToAmount(coinData.getBalanceInInternalUnits());
            } else if (amount.getCurrency().equals("ETH") && coinData.getBalanceInInternalUnits().isZero()) {
                balance = convertToAmount(coinData.getBalanceAlterInInternalUnits());
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return amount.compareTo(balance) <= 0;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
        if (!hasBalanceInfo()) return false;

        try {
            Amount balance = convertToAmount(coinData.getBalanceAlterInInternalUnits());

            if (fee == null || amount == null || fee.isZero() || amount.isZero())
                return false;


            if (amount.getCurrency().equals(ctx.getCard().tokenSymbol)) {
                // token transaction
                if (fee.compareTo(balance) > 0)
                    return false;
            } else if (amount.getCurrency().equals("ETH") && coinData.getBalanceInInternalUnits().isZero()) {
                // standard ETH transaction
                try {
                    BigDecimal cardBalance = getBalance();

                    if (isFeeIncluded && amount.compareTo(cardBalance) > 0)
                        return false;

                    if (!isFeeIncluded && amount.add(fee).compareTo(cardBalance) > 0)
                        return false;

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else

            {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (getBalance() == null) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Unknown balance");
            balanceValidator.setSecondLine("Balance cannot be verified. Swipe down to refresh.");
            return false;
        }

        if (!coinData.getUnconfirmedTXCount().equals(coinData.getConfirmedTXCount())) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Unguaranteed balance");
            balanceValidator.setSecondLine("Transaction is in progress. Wait for confirmation in blockchain.");
            return false;
        }

        if (coinData.isBalanceReceived()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine("Verified balance");
            balanceValidator.setSecondLine("Balance confirmed in blockchain");
            if (getBalance().isZero()) {
                balanceValidator.setFirstLine("Empty wallet");
                balanceValidator.setSecondLine("");
            }
        }

        if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && getBalance().notZero()) {
            balanceValidator.setScore(80);
            balanceValidator.setFirstLine("Verified offline balance");
            balanceValidator.setSecondLine("Restore internet connection to obtain trusted balance from blockchain");
        }

        return true;
    }


    @Override
    public String evaluateFeeEquivalent(String fee) {
        try {
            Amount feeValue = new Amount(fee, getFeeCurrency());
            return feeValue.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

//
//        BigDecimal gweFee = new BigDecimal(fee);
//        gweFee = gweFee.divide(new BigDecimal("1000000000"));
//        gweFee = gweFee.setScale(18, RoundingMode.DOWN);
//        return getFeeEquivalentDescriptor(mCard, gweFee.toString());
    }

    @Override
    public SignTask.PaymentToSign constructPayment(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        if (amountValue.getCurrency().equals("ETH")) {
            return constructPaymentETH(feeValue, amountValue, IncFee, targetAddress);
        } else {
            return constructPaymentToken(feeValue, amountValue, IncFee, targetAddress);
        }
    }

    private SignTask.PaymentToSign constructPaymentETH(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress) throws Exception {
        BigInteger nonceValue = coinData.getConfirmedTXCount();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();

        BigInteger weiFee = convertToInternalAmount(feeValue).toBigIntegerExact();
        BigInteger weiAmount = convertToInternalAmount(amountValue).toBigIntegerExact();

        if (IncFee) {
            weiAmount = weiAmount.subtract(weiFee);
        }

        BigInteger gasPrice = weiFee.divide(BigInteger.valueOf(21000));
        BigInteger gasLimit = BigInteger.valueOf(21000);
//        Integer chainId = ctx.getBlockchain() == Blockchain.Ethereum ? EthTransaction.ChainEnum.Mainnet.getValue() : EthTransaction.ChainEnum.Rinkeby.getValue();
        Integer chainId = EthTransaction.ChainEnum.Mainnet.getValue(); // Token support on main net only!!!

        String to = targetAddress;

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        final EthTransaction tx = EthTransaction.create(to, weiAmount, nonceValue, gasPrice, gasLimit, chainId);

        return new SignTask.PaymentToSign() {
            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] hashesForSign = new byte[1][];
                hashesForSign[0] = tx.getRawHash();
                return hashesForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for ETH");
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for ETH");
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                byte[] for_hash = tx.getRawHash();
                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
                s = CryptoUtil.toCanonicalised(s);

                boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);

                if (!f) {
                    Log.e("ETH-CHECK", "sign Failed.");
                }

                tx.signature = new ECDSASignatureETH(r, s);
                int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
                if (v != 27 && v != 28) {
                    Log.e("ETH", "invalid v");
                    throw new Exception("Error in EthEngine - invalid v");
                }
                tx.signature.v = (byte) v;
                Log.e("ETH_v", String.valueOf(v));

                byte[] txForSend = tx.getEncoded();
                notifyOnNeedSendPayment(txForSend);
                return txForSend;
            }
        };
    }

    private SignTask.PaymentToSign constructPaymentToken(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress) throws Exception {
        BigInteger nonceValue = coinData.getConfirmedTXCount();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();
//        boolean flag = (ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer);
//        Issuer issuer = ctx.getCard().getIssuer();


//        BigInteger gigaK = BigInteger.valueOf(1000000000L);

        BigInteger weiFee = convertToInternalAmount(feeValue).toBigIntegerExact();

        InternalAmount amountDec = convertToInternalAmount(amountValue);
        BigInteger amount = amountDec.toBigInteger(); //new BigInteger(amountValue, 10);


        //amount = amount.subtract(fee);

        BigInteger gasPrice = weiFee.divide(BigInteger.valueOf(60000));
        BigInteger gasLimit = BigInteger.valueOf(60000);
        Integer chainId = EthTransaction.ChainEnum.Mainnet.getValue();
        BigInteger amountZero = BigInteger.ZERO;

        String to = targetAddress;

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        String contractAddress = getContractAddress(ctx.getCard());

        if (contractAddress.startsWith("0x") || contractAddress.startsWith("0X")) {
            contractAddress = contractAddress.substring(2);
        }

        String amountLeadZero = amount.toString(16);
        if (amountLeadZero.startsWith("0x") || amountLeadZero.startsWith("0X")) {
            amountLeadZero = amountLeadZero.substring(2);
        }

        while (amountLeadZero.length() < 64) {
            amountLeadZero = "0" + amountLeadZero;
        }

        String cmd = "a9059cbb000000000000000000000000" + to + amountLeadZero; //TODO only for BAT


        byte[] data = BTCUtils.fromHex(cmd);
        EthTransaction tx = EthTransaction.create(contractAddress, amountZero, nonceValue, gasPrice, gasLimit, chainId, data);

        return new SignTask.PaymentToSign() {
            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] hashesForSign = new byte[1][];
                hashesForSign[0] = tx.getRawHash();
                return hashesForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for ETH");
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for ETH");
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                byte[] for_hash = tx.getRawHash();
                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
                s = CryptoUtil.toCanonicalised(s);

                boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);

                if (!f) {
                    Log.e("ETH-CHECK", "sign Failed.");
                }

                tx.signature = new ECDSASignatureETH(r, s);
                int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
                if (v != 27 && v != 28) {
                    Log.e("ETH", "invalid v");
                    throw new Exception("Error in EthEngine - invalid v");
                }
                tx.signature.v = (byte) v;
                Log.e("ETH_v", String.valueOf(v));

                byte[] txForSend = tx.getEncoded();
                notifyOnNeedSendPayment(txForSend);
                return txForSend;

            }
        };

    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiInfura serverApiInfura = new ServerApiInfura();
        // request infura listener
        ServerApiInfura.InfuraBodyListener infuraBodyListener = new ServerApiInfura.InfuraBodyListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                switch (method) {
                    case ServerApiInfura.INFURA_ETH_GET_BALANCE: {
                        String balanceCap = infuraResponse.getResult();
                        balanceCap = balanceCap.substring(2);
                        BigInteger l = new BigInteger(balanceCap, 16);
                        coinData.setBalanceReceived(true);
                        coinData.setBalanceAlterInInternalUnits(new CoinEngine.InternalAmount(l, "wei"));

//                        Log.i("$TAG eth_get_balance", balanceCap)
                    }
                    break;

                    case ServerApiInfura.INFURA_ETH_GET_TRANSACTION_COUNT: {
                        String nonce = infuraResponse.getResult();
                        nonce = nonce.substring(2);
                        BigInteger count = new BigInteger(nonce, 16);
                        coinData.setConfirmedTXCount(count);


//                        Log.i("$TAG eth_getTransCount", nonce)
                    }
                    break;

                    case ServerApiInfura.INFURA_ETH_GET_PENDING_COUNT: {
                        String pending = infuraResponse.getResult();
                        pending = pending.substring(2);
                        BigInteger count = new BigInteger(pending, 16);
                        coinData.setUnconfirmedTXCount(count);

//                        Log.i("$TAG eth_getPendingTxCount", pending)
                    }
                    break;
//
                    case ServerApiInfura.INFURA_ETH_CALL: {
                        try {
                            String balanceCap = infuraResponse.getResult();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            Long balance = l.longValue();
                            coinData.setBalanceInInternalUnits(new CoinEngine.InternalAmount(l, ctx.getCard().tokenSymbol));

//                            Log.i("$TAG eth_call", balanceCap)

                            if (blockchainRequestsCallbacks.allowAdvance()) {
                                serverApiInfura.infura(ServerApiInfura.INFURA_ETH_GET_BALANCE, 67, coinData.getWallet(), "", "");
                                serverApiInfura.infura(ServerApiInfura.INFURA_ETH_GET_TRANSACTION_COUNT, 67, coinData.getWallet(), "", "");
                                serverApiInfura.infura(ServerApiInfura.INFURA_ETH_GET_PENDING_COUNT, 67, coinData.getWallet(), "", "");
                            } else {
                                ctx.setError("Terminated by user");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                }
                if (serverApiInfura.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onFail(String method, String message) {
                if (!serverApiInfura.isRequestsSequenceCompleted()) {
                    ctx.setError(message);
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }
        };
        serverApiInfura.setInfuraResponse(infuraBodyListener);

        serverApiInfura.infura(ServerApiInfura.INFURA_ETH_CALL, 67, coinData.getWallet(), getContractAddress(ctx.getCard()), "");
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        ServerApiInfura serverApiInfura = new ServerApiInfura();
        // request infura eth gasPrice listener
        ServerApiInfura.InfuraBodyListener infuraBodyListener = new ServerApiInfura.InfuraBodyListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                String gasPrice = infuraResponse.getResult();
                gasPrice = gasPrice.substring(2);
                // rounding gas price to integer gwei
                BigInteger l = new BigInteger(gasPrice, 16).divide(BigInteger.valueOf(1000000000L)).multiply(BigInteger.valueOf(1000000000L));

                //val m = if (ctx.blockchain==Blockchain.Token) BigInteger.valueOf(60000) else BigInteger.valueOf(21000)
                BigInteger m;
                if (amount.getCurrency().equals("ETH")) m = BigInteger.valueOf(60000);
                else m = BigInteger.valueOf(21000);

                CoinEngine.InternalAmount weiMinFee = new CoinEngine.InternalAmount(l.multiply(m), "wei");
                CoinEngine.InternalAmount weiNormalFee = new CoinEngine.InternalAmount(weiMinFee.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(10)), "wei");
                CoinEngine.InternalAmount weiMaxFee = new CoinEngine.InternalAmount(weiMinFee.multiply(BigDecimal.valueOf(15)).divide(BigDecimal.valueOf(10)), "wei");

                try {
                    coinData.minFee = convertToAmount(weiMinFee);
                    coinData.normalFee = convertToAmount(weiNormalFee);
                    coinData.maxFee = convertToAmount(weiMaxFee);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                blockchainRequestsCallbacks.onComplete(true);
            }

            @Override
            public void onFail(String method, String message) {
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApiInfura.setInfuraResponse(infuraBodyListener);

        serverApiInfura.infura(ServerApiInfura.INFURA_ETH_GAS_PRICE, 67, coinData.getWallet(), "", "");
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws Exception {

        String txStr = String.format("0x%s", BTCUtils.toHex(txForSend));

        ServerApiInfura serverApiInfura = new ServerApiInfura();
        // request infura eth gasPrice listener
        ServerApiInfura.InfuraBodyListener infuraBodyListener = new ServerApiInfura.InfuraBodyListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                if (method.equals(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION)) {
                    if (infuraResponse.getResult().isEmpty()) {
                        ctx.setError("Rejected by node: " + infuraResponse.getError());
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        BigInteger nonce = coinData.getConfirmedTXCount();
                        nonce.add(BigInteger.valueOf(1));
                        coinData.setConfirmedTXCount(nonce);
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    }
                }
            }

            @Override
            public void onFail(String method, String message) {
                if (method.equals(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION)) {
                    ctx.setError(message);
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }
        };

        serverApiInfura.setInfuraResponse(infuraBodyListener);

        serverApiInfura.infura(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION, 67, coinData.getWallet(), "", txStr);

    }


//    public byte[] signETH(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress, CardProtocol protocol) throws Exception {
//        BigInteger nonceValue = coinData.getConfirmedTXCount();
//        byte[] pbKey = ctx.getCard().getWalletPublicKey();
////        boolean flag = (ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer);
//        Issuer issuer = ctx.getCard().getIssuer();
//
//        BigInteger weiFee = convertToInternalAmount(feeValue).toBigIntegerExact();
//        BigInteger weiAmount = convertToInternalAmount(amountValue).toBigIntegerExact();
//
//        if (IncFee) {
//            weiAmount = weiAmount.subtract(weiFee);
//        }
//
//        BigInteger nonce = nonceValue;
//        BigInteger gasPrice = weiFee.divide(BigInteger.valueOf(21000));
//        BigInteger gasLimit = BigInteger.valueOf(21000);

//        Integer chainId = ctx.getBlockchain() == Blockchain.Ethereum ? EthTransaction.ChainEnum.Mainnet.getValue() : EthTransaction.ChainEnum.Rinkeby.getValue();
//        Integer chainId = EthTransaction.ChainEnum.Mainnet.getValue(); // Token support on main net only!!!
//
//        String to = targetAddress;
//
//        if (to.startsWith("0x") || to.startsWith("0X")) {
//            to = to.substring(2);
//        }
//
//        EthTransaction tx = EthTransaction.create(to, weiAmount, nonce, gasPrice, gasLimit, chainId);
//
//        byte[][] hashesForSign = new byte[1][];
//        byte[] for_hash = tx.getRawHash();
//        hashesForSign[0] = for_hash;
//
//        byte[] signFromCard = null;
//        try {
//            signFromCard = protocol.run_SignHashes(PINStorage.getPIN2(), hashesForSign, null, null, null).getTLV(TLV.Tag.TAG_Signature).Value;
//            // TODO slice signFromCard to hashes.length parts
//        } catch (Exception ex) {
//            Log.e("ETH", ex.getMessage());
//            return null;
//        }
//
//        BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
//        BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
//        s = CryptoUtil.toCanonicalised(s);
//
//        boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);
//
//        if (!f) {
//            Log.e("ETH-CHECK", "sign Failed.");
//        }
//
//        tx.signature = new ECDSASignatureETH(r, s);
//        int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
//        if (v != 27 && v != 28) {
//            Log.e("ETH", "invalid v");
//            return null;
//        }
//        tx.signature.v = (byte) v;
//        Log.e("ETH_v", String.valueOf(v));
//
//        byte[] realTX = tx.getEncoded();
//        return realTX;
//    }

//    public byte[] signToken(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress, CardProtocol protocol) throws Exception {
//        BigInteger nonceValue = coinData.getConfirmedTXCount();
//        byte[] pbKey = ctx.getCard().getWalletPublicKey();
////        boolean flag = (ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer);
//        Issuer issuer = ctx.getCard().getIssuer();
//
//
//        BigInteger gigaK = BigInteger.valueOf(1000000000L);
//
//        BigInteger weiFee = convertToInternalAmount(feeValue).toBigIntegerExact();
//
//        InternalAmount amountDec=convertToInternalAmount(amountValue);
//        BigInteger amount = amountDec.toBigInteger(); //new BigInteger(amountValue, 10);
//
//
//        //amount = amount.subtract(fee);
//
//        BigInteger nonce = nonceValue;
//        BigInteger gasPrice = weiFee.divide(BigInteger.valueOf(60000));
//        BigInteger gasLimit = BigInteger.valueOf(60000);
//        Integer chainId = EthTransaction.ChainEnum.Mainnet.getValue();
//        BigInteger amountZero = BigInteger.ZERO;
//
//        String to = targetAddress;
//
//        if (to.startsWith("0x") || to.startsWith("0X")) {
//            to = to.substring(2);
//        }
//
//        String contractAddress = getContractAddress(ctx.getCard());
//
//        if (contractAddress.startsWith("0x") || contractAddress.startsWith("0X")) {
//            contractAddress = contractAddress.substring(2);
//        }
//
//        String amountLeadZero = amount.toString(16);
//        if (amountLeadZero.startsWith("0x") || amountLeadZero.startsWith("0X")) {
//            amountLeadZero = amountLeadZero.substring(2);
//        }
//
//        while (amountLeadZero.length() < 64) {
//            amountLeadZero = "0" + amountLeadZero;
//        }
//
//        String cmd = "a9059cbb000000000000000000000000" + to + amountLeadZero; //TODO only for BAT
//
//
//        byte[] data = BTCUtils.fromHex(cmd);
//        EthTransaction tx = EthTransaction.create(contractAddress, amountZero, nonce, gasPrice, gasLimit, chainId, data);
//
//        byte[][] hashesForSign = new byte[1][];
//        byte[] for_hash = tx.getRawHash();
//        hashesForSign[0] = for_hash;
//
//        byte[] signFromCard = null;
//        try {
//            signFromCard = protocol.run_SignHashes(PINStorage.getPIN2(), hashesForSign, null, null, null).getTLV(TLV.Tag.TAG_Signature).Value;
//            // TODO slice signFromCard to hashes.length parts
//        } catch (Exception ex) {
//            Log.e("ETH", ex.getMessage());
//            return null;
//        }
//
//        BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
//        BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
//        s = CryptoUtil.toCanonicalised(s);
//
//        boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);
//
//        if (!f) {
//            Log.e("ETH-CHECK", "sign Failed.");
//        }
//
//        tx.signature = new ECDSASignatureETH(r, s);
//        int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
//        if (v != 27 && v != 28) {
//            Log.e("ETH", "invalid v");
//            return null;
//        }
//        tx.signature.v = (byte) v;
//        Log.e("ETH_v", String.valueOf(v));
//
//        byte[] realTX = tx.getEncoded();
//        return realTX;
//    }
}
