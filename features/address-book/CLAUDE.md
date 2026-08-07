# Address Book Feature

Per-wallet recipient contacts (name, avatar color, a set of `address + network (+ memo)` entries). Opened two
ways: standalone from Settings (browse/manage), and embedded in the Send screen as a recipient picker that
returns a chosen address back to Send.

## Navigation

Entry point `AddressBookComponent`; the `AddressBookOpenMode` (in `common/routing`, via `AppRoute.AddressBook`)
decides the initial screen:
- `Default` — browse/manage from Settings.
- `ContactSelection(networkId)` — from Send; a tap returns an address for that network.
- `WithContactCreation(address, networkId)` — from Send-success; opens a new contact with the address attached.

`DefaultAddressBookComponent` owns a single `childStack(AddressBookRoute)`. Children never reference each other:
`AddressBookChildFactory` builds each one and wires its callbacks to one `AddressBookClickIntents` object the
container implements.

```
AddressBookRoute
  ├─ List(mode)        → AddressBookListModel     (mode Default vs Selector only changes what a tap does)
  ├─ EditContact(...)  → EditContactModel         (+ childSlot: PortfolioSelector & AddressInfo bottom sheets)
  ├─ AddAddress(...)   → AddAddressModel
  └─ SelectNetworks    → SelectNetworksModel
```

Two more components are used **outside** this stack, embedded directly in the Send screen (not reached via
`AddressBookRoute`): `AddressBookContactsBlockComponent` (the up-to-5 contacts block) and
`AddressSelectorComponent` (bottom sheet to pick one of several addresses of a contact).

## Cross-scope result delivery

Screens live in independent model scopes, so results are handed over out-of-band, not through nav args. Three
separate singleton mechanisms, each with its own contract:

- **`AddressBookResultHolder`** (`StateFlow`): AddAddress → EditContact. `ConfirmedAddress.replaces` carries the
  entry being superseded in the edit-address flow (swap-in-place). Producer sets, consumer observes then
  `clear()`s so it isn't re-applied on resubscription.
- **`SelectNetworksResultHolder`** (`StateFlow`): SelectNetworks → AddAddress, same produce→observe→`clear()`.
- **`DefaultContactSelectionTrigger`** (implements `ContactSelectionTrigger` + `ContactSelectionListener`):
  full-list pick → Send. Deliberately a **no-replay `SharedFlow`** (1-item buffer, `tryEmit`), not a holder —
  a one-shot signal with nothing retained, so the picker can close even if Send isn't collecting, and there is
  nothing to `clear()`.

Holders are `clear()`ed at the start of a session (`DefaultAddressBookComponent.init`, `AddAddressModel.init`)
so a leftover result from a previous session isn't replayed.

## Gotchas

**Memo prefill can't be written synchronously.** In the edit-address flow, address validation is debounced, so
right after `prefillData()` no network is selected yet and `UpdateAddressValidationTransformer` keeps the memo
field hidden and **blanks its value** (`resolveMemoField` clears `value=""` whenever no extras-supporting network
is selected). Setting the memo eagerly is therefore wiped before the field is shown.
`AddAddressModel.prefillMemoOnceVisible` waits for `memoField.isVisible` to flip true, then writes the memo once.
Preserve this ordering when touching prefill/validation (regression test: `Prefill › GIVEN prefilled memo on
extras network …`).

**`resolveMemoField` clears the memo value on hide by design.** Deselecting the extras network (or changing to a
non-extras chain) intentionally blanks the memo; `validateAndConfirm` guards with `isVisible && isNotEmpty` so a
stale hidden value never leaks into a `ValidatedAddress`.

**`ContactAddressEntriesConverter` collapses entries by address and takes the first non-null memo of the group.**
There is no per-network memo — if that ever changes, this converter (and `ValidatedAddress.memo`) needs revisiting.