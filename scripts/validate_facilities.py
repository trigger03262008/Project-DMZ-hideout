#!/usr/bin/env python3
"""Independent static checks for the v0.3.1 standalone facility slice."""
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
def read(path):
    return (root/path).read_text(encoding="utf-8")
def js(path):
    return json.loads(read(path))

names=("stash","med_station","trading_center")
registry=read("src/main/java/dev/dmzhideout/block/HideoutBlocks.java")
for name in names:
    assert '"' + name + '"' in registry, "Missing registry: "+name
    for path in (
        f"src/main/resources/assets/dmzhideout/blockstates/{name}.json",
        f"src/main/resources/assets/dmzhideout/models/block/{name}.json",
        f"src/main/resources/assets/dmzhideout/models/item/{name}.json",
        f"src/main/resources/data/dmzhideout/loot_tables/blocks/{name}.json",
    ):
        assert js(path), path
    loot=js(f"src/main/resources/data/dmzhideout/loot_tables/blocks/{name}.json")
    assert loot["pools"][0]["entries"][0]["name"] == f"dmzhideout:{name}"
    assert js(f"src/main/resources/assets/dmzhideout/blockstates/{name}.json")["variants"][""]["model"] == f"dmzhideout:block/{name}"
    assert js(f"src/main/resources/assets/dmzhideout/models/item/{name}.json")["parent"] == f"dmzhideout:block/{name}"

for locale in ("en_us","ja_jp"):
    lang=js(f"src/main/resources/assets/dmzhideout/lang/{locale}.json")
    for name in names:
        assert lang[f"block.dmzhideout.{name}"]

recipe=js("src/main/resources/data/dmzhideout/med_recipes/bandage.json")
assert recipe["id"]=="dmzhideout:bandage"
assert recipe["required_mods"]==["dmzcore"]
assert recipe["output"]=="dmzcore:bandage"
assert recipe["count"]==1
assert len(recipe["ingredients"])>=1
assert all(i["count"]>0 and ":" in i["item"] for i in recipe["ingredients"])

assert js("src/main/resources/pack.mcmeta")["pack"]["pack_format"]==15
assert "project-dmz-hideout-0.3.1" in read(".github/workflows/hideout-ci.yml")
assert "version = '0.3.1'" in read("build.gradle")
source=read("src/main/java/dev/dmzhideout/block/StashBlockEntity.java")
assert "MAX_LEVEL = 5" in source and "SLOTS_PER_LEVEL = 270" in source
for token in ("setChanged()","getIntArray(\"Locked\")","toggleShared","upgrade","insert("):
    assert token in source
bridge=read("src/main/java/dev/dmzhideout/api/StashApi.java")
for method in ("findAdjacent","canInsert","insert","extract","snapshot","restore","spendableSnapshot"):
    assert method in bridge

print("Hideout v0.3.1 facility assets, medical recipes and storage bridge: OK")

# Regression: custom grid selection and basket controls precede vanilla
# container-slot handling. Physical Stash items are never removed by click.
screen=read("src/main/java/dev/dmzhideout/client/TradeScreen.java")
assert screen.index("private int rightVisible(")<screen.index("return super.mouseClicked(mx,my,button);")
assert screen.index("private int basketVisible(")<screen.index("return super.mouseClicked(mx,my,button);")
assert "TRADE_BASKET_ADD" in screen and "TRADE_BATCH_SELL" in screen
assert "TRADERS_PER_PAGE=5" in screen
assert "pendingStash" in screen
stash=read("src/main/java/dev/dmzhideout/menu/StashMenu.java")
assert "ScrollMath.maxScroll" in stash
assert "EquipmentSlot.HEAD" in stash and "EquipmentSlot.FEET" in stash
assert "inventory,40" in stash
medical=read("src/main/java/dev/dmzhideout/med/MedRecipes.java")
assert "stashFirst" in medical and "StashApi.restore" in medical
medui=read("src/main/java/dev/dmzhideout/client/MedScreen.java")
assert "CRAFT / 製造" in medui and "HEAL / 治療" in medui
print("Hideout v0.3.1 Med/Stash/Trade UI contracts: OK")

# Hotfix: healing quote must not depend on PDA cash initialization, legacy
# Health state adapters must be tried when the public treatment API is absent,
# and both GUI screens show the synced PDA balance without trusting clients.
compat=read("src/main/java/dev/dmzhideout/compat/MedicalCompat.java")
cash=read("src/main/java/dev/dmzhideout/client/CashDisplay.java")
med=read("src/main/java/dev/dmzhideout/client/MedScreen.java")
trade=read("src/main/java/dev/dmzhideout/client/TradeScreen.java")
assert "legacyQuote(" in compat and "legacyHeal(" in compat
assert "quoteFullTreatment" in compat and "healFully" in compat
assert "CashApi" in compat and "trySpend" in compat and "credit" in compat
assert "ClientCashState" in cash and '"balance"' in cash
assert "CASH: "+CashDisplay.format()' in med and "CASH: "+CashDisplay.format()' in trade
assert "healButton.active=menu.quote()>0 && CashDisplay.balance()>=menu.quote()" in med
print("v0.3.1 healing and CASH UI contract: OK")
