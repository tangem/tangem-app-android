package com.tangem.tangem_card.reader;

/**
 * Created by dvol on 15.07.2019.
 */

public class ProductMask {
    public static final int Note = 0x0001;
    public static final int Tag = 0x0002;
    public static final int IdCard = 0x0004;
    public static final int IdIssuer = 0x0008;

    public static String getDescription(int iValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if ((iValue & ProductMask.Note) != 0) sb.append("Note, ");
        if ((iValue & ProductMask.Tag) != 0)
            sb.append("Tag, ");
        if ((iValue & ProductMask.IdCard) != 0)
            sb.append("IdCard, ");
        if ((iValue & ProductMask.IdIssuer) != 0)
            sb.append("IdIssuer, ");

        if (sb.length() > 1) sb.delete(sb.length() - 2, sb.length());
        sb.append("]");
        return sb.toString();
    }
}
