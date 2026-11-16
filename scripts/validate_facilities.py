#!/usr/bin/env python3
"""Independent static checks for the v0.2.0 standalone facility slice."""
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
assert "project-dmz-hideout-0.2.0" in read(".github/workflows/hideout-ci.yml")
assert "version = '0.2.0'" in read("build.gradle")
source=read("src/main/java/dev/dmzhideout/block/StashBlockEntity.java")
assert "MAX_LEVEL = 5" in source and "SLOTS_PER_LEVEL = 270" in source
for token in ("setChanged()","getIntArray(\"Locked\")","toggleShared","upgrade","insert("):
    assert token in source
bridge=read("src/main/java/dev/dmzhideout/api/StashApi.java")
for method in ("findAdjacent","canInsert","insert","extract","snapshot","restore","spendableSnapshot"):
    assert method in bridge

print("Hideout v0.2.0 facility assets, medical recipes and storage bridge: OK")

# Custom left-hand offer clicks MUST be prioritized over Minecraft vanilla
# AbstractContainerScreen handling (which may consume unhandled clicks).
screen=read("src/main/java/dev/dmzhideout/client/TradeScreen.java")
custom=screen.index("TradeOfferSelection.offerIndex(")
vanilla=screen.index("return super.mouseClicked(mx,my,button);")
assert custom<vanilla
assert "selectedPurchase=selected.purchase()" in screen
assert "boolean selected=offer.purchase()==selectedPurchase" in screen
print("Trading Center UI event ordering: OK")
