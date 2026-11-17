#!/usr/bin/env python3
"""Release contract for Project DMZ: Loot v0.1.0."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def data(path): return json.loads((root/path).read_text(encoding="utf-8"))
catalog=data("src/main/resources/data/dmzloot/catalog/items.json")
assert len(catalog)==32, len(catalog)
by_id={x["id"]:x for x in catalog}
assert len(by_id)==32
base={"scrap_iron","scrap_copper","scrap_plastic","scrap_cloth",
 "bolt","nut","screw","spring","copper_wire","screwdriver",
 "broken_circuit_board","broken_radio","scrap_rubber","scrap_glass",
 "insulation_tape","capacitor","battery","small_motor","wrench","pliers",
 "sewing_kit","broken_alarm_clock","pocket_watch","silver_ring"}
assert len(base)==24 and base<=by_id.keys()
en=data("src/main/resources/assets/dmzloot/lang/en_us.json")
ja=data("src/main/resources/assets/dmzloot/lang/ja_jp.json")
for item in catalog:
    id=item["id"]
    assert item["mass_kg"]>=0
    assert 1<=item["stack_size"]<=64
    assert all(1<=g<=8 for g in item["grid"])
    assert item["base_cash"]>=0
    if "tool_family" in item:
        assert item["stack_size"]==1 and item["tier"] in (0,1,2)
        assert item["max_durability"]>0
    assert "item.dmzloot."+id in en and "item.dmzloot."+id in ja
    model=data("src/main/resources/assets/dmzloot/models/item/"+id+".json")
    assert model["textures"]["layer0"]=="dmzloot:item/"+id
for source in ("broken_radio","broken_circuit_board","broken_alarm_clock"):
    recipe=data("src/main/resources/data/dmzloot/salvage/"+source+".json")
    assert recipe["item"]=="dmzloot:"+source
    assert len(recipe["tools"])==1
    assert recipe["tools"][0]["family"] in ("screwdriver","pliers")
    assert recipe["tools"][0]["min_tier"] in (0,1,2)
    assert 1<=recipe["tools"][0]["wear"]<=50
    total=0
    for output in recipe["outputs"]:
        name=output["item"].split(":")[1]
        assert name in by_id
        assert 1<=output["count"]<=by_id[name]["stack_size"]
        total+=by_id[name]["base_cash"]*output["count"]
    assert total < by_id[source]["base_cash"],(source,total)
for t in ("screwdriver","wrench","pliers","sewing_kit"):
    assert by_id["improvised_"+t]["tier"]==0
    assert by_id[t]["tier"]==1
    assert by_id["professional_"+t]["tier"]==2
    rec=data("src/main/resources/data/dmzloot/recipes/improvised_"+t+".json")
    assert rec["result"]["item"]=="dmzloot:improvised_"+t
    assert all(ingredient["item"].split(":")[1] in by_id for ingredient in rec["ingredients"])
print("Loot v0.1.0 catalog (32), locale, icons, salvage economics and crude-tool recipes: OK")
