package com.ripple.core.types.known.tx.result;

import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt16;
import com.ripple.core.fields.Field;
import com.ripple.core.serialized.SerializedType;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.types.known.sle.LedgerEntry;

// TODO: fix up this nonsense
public class AffectedNode extends STObject {
    public final Field field;
    public final STObject nested;

    public AffectedNode(STObject source) {
        fields = source.getFields();
        field = getField();
        nested = nestedObject();
    }

    public boolean isOffer() {
        return ledgerEntryType() == LedgerEntryType.Offer;
    }

    public boolean isAccountRoot() {
        return ledgerEntryType() == LedgerEntryType.AccountRoot;
    }

    public boolean isRippleState() {
        return ledgerEntryType() == LedgerEntryType.RippleState;
    }

    public boolean isDirectoryNode() {
        return ledgerEntryType() == LedgerEntryType.DirectoryNode;
    }

    public boolean wasPreviousNode() {
        return isDeletedNode() || isModifiedNode();
    }

    public boolean isFinalNode() {
        return true;
    }

    public boolean isCreatedNode() {
        return field == Field.CreatedNode;
    }

    public boolean isDeletedNode() {
        return field == Field.DeletedNode;
    }

    public boolean isModifiedNode() {
        return field == Field.ModifiedNode;
    }

    public Field getField() {
//        return iterator().next();
         return fields.firstKey();
    }

    public Hash256 ledgerIndex() {
        return nested.get(Hash256.LedgerIndex);
    }

    public LedgerEntryType ledgerEntryType() {
        return ledgerEntryType(nested);
    }

    private STObject nestedObject() {
        return (STObject) get(getField());
    }

    /**
     * @return - LedgerEntry before the transaction (or after in the case of
     *           a CreatedNode)
     */
    public LedgerEntry nodeAsPrevious() {
        return (LedgerEntry) rebuildFromMeta(true);
    }

    public LedgerEntry nodeAsFinal() {
        return (LedgerEntry) rebuildFromMeta(false);
    }

    private STObject rebuildFromMeta(boolean asPrevious) {
        boolean created = isCreatedNode();
        STObject mixed = new STObject();

        // The first object has only a single key
        Field wrapperField = created ? Field.CreatedNode :
                isDeletedNode() ? Field.DeletedNode :
                        Field.ModifiedNode;

        STObject wrapped = (STObject) get(wrapperField);

        Field finalFields = created ? Field.NewFields :
                Field.FinalFields;

        // You may get some AccountRoot objects like this
        if (!wrapped.has(finalFields)) {
            STObject source = new STObject(wrapped.getFields());
            source.put(Hash256.index, wrapped.get(Hash256.LedgerIndex));
            source.remove(Field.LedgerIndex);
            return STObject.formatted(source);
        }

        // Get all the final fields
        STObject finals = (STObject) wrapped.get(finalFields);
        for (Field field : finals) {
            mixed.put(field, finals.get(field));
        }

        // Then layer over the previous fields if desired as previous
        // DirectoryNode LedgerEntryType won't have `PreviousFields`
        if (asPrevious && wrapped.has(Field.PreviousFields)) {
            STObject previous = wrapped.get(STObject.PreviousFields);
            for (Field field : previous) {
                mixed.put(field, previous.get(field));
            }
        }

        // Keep the inner most fields
        for (Field field : wrapped) {
            switch (field) {
                case NewFields:
                case PreviousFields:
                case FinalFields:
                    continue;
                default:
                    SerializedType value = wrapped.get(field);
                    if (field == Field.LedgerIndex) {
                        field = Field.index;
                    }
                    mixed.put(field, value);

            }
        }
        return STObject.formatted(mixed);
    }

    public static boolean isAffectedNode(STObject source) {
        return (source.size() == 1 && (
                source.has(DeletedNode) ||
                source.has(CreatedNode) ||
                source.has(ModifiedNode)));
    }

    public boolean removedField(Field field) {
        return nested.has(Field.PreviousFields) &&
                nested.get(STObject.PreviousFields).has(field) &&
                nested.has(Field.FinalFields) &&
                !nested.get(STObject.FinalFields).has(field);
    }
}
