<!-- Source: ExtraNerf 1.1.1, ba670257e49cd7b25153e63678e8ceb11f1cf7f1 -->
# Complete gameplay change catalog

This page lists every gameplay change implemented by the current source. It
describes the resulting behavior, not removed historical experiments.

Percent chances use GregTech's 10,000-point convention: 1,000 is 10%, 500 is
5%, and 10 is 0.1%.

## Blighted Soybean

HarvestCraft soybean crops yield Blighted Soybean where they would otherwise
drop Soybean. It is not edible, has no recipes added by Extra Nerf and is hidden
from NEI's item list.

## Floral Fertilizer on LV

A shaped crafting recipe combines one Quick and Dirty Portal Generator,
two Alumentum and six CropsNH Fertilizers into six Floral Fertilizers:

```text
F A F
F P F
F A F
```

`P` is the Portal Generator, `A` is Alumentum and `F` is CropsNH Fertilizer.
The consumed Portal Generator supplies the LV Assembler requirement and cost.
This provides a Botania entry route for GoG and dimension-restricted runs,
breaking the dependency cycle between MV motors, Manasteel and mystical flowers.

## Livingrock Plate

An electric Forge Hammer converts one Livingrock and 22 L of Lubricant into
one Livingrock Plate in 150 seconds at 30 EU/t. This provides an LV route to
the Diluted Mana Pool without requiring a Cutting Machine.

## Metal transmutation

Crucible recipes that duplicate a GregTech metal nugget consume one nugget and
four times their original aspect cost, producing two Tiny Piles of that metal's
Dust. This applies to the separate Thaumcraft and GregTech metal researches as
well as Witching Gadgets' Metallurgic Perfection. For example, Aluminium
Transmutation uses one Aluminium Nugget, 8 Metallum, 4 Volatus, 4 Ordo and
4 Ignis to produce two Tiny Piles of Aluminium Dust.

GregTech's separate Steel Transmutation consumes one Iron Ingot and 4 Ordo to
produce one Tiny Pile of Steel Dust. Recipes for non-metal materials remain
unchanged.

## Assembly Lines in AE2 Spatial worlds

The native Assembly Line and GigaGramFab Advanced Assembly Line form only in
AE2 Spatial Storage worlds. The server recognizes AE2's world provider instead
of comparing a fixed dimension number, so independently created cells work.

Native structure checks run first. Only an otherwise valid machine in the wrong
world receives the controller's hidden "more fluixy" structure error. Incomplete
structures retain their ordinary diagnostics. Tooltips, NEI and construction
hints do not reveal this condition. Native GUI synchronization carries the
error to clients; processing remains controlled by the server's formed state.

Both machines have a 3-by-4 cross section and can reach 16 slices. A 2-cubed
Spatial cell is too small; 16-cubed storage accommodates either machine.
The standard 5-slice build leaves space for connections. Component Assembly
Line and Circuit Assembly Line are outside this rule.

Startup auditing verifies both native hooks and registrations, plus the English
error resource. Integration fixtures build both machines, reject complete lines
in the Overworld without consuming inputs, hide the error on broken structures,
serialize it to the client and process a native recipe inside real AE2-created
16-cubed Spatial worlds.

## Void Miner gas consumption

All three Void Miners accept all four native noble gases. Instead of rejecting
gases by tier, they multiply the native gas cost as follows:

| Gas | Retained ore boost | MK1 / LuV | MK2 / ZPM | MK3 / UV |
| --- | ---: | ---: | ---: | ---: |
| Neon | 4x | 10x | 100x | 1,000x |
| Krypton | 8x | 1,000x | 10x | 100x |
| Xenon | 16x | 100,000x | 1,000x | 10x |
| Oganesson | 64x | 10,000,000x | 100,000x | 10x |

The same multiplied amount is required and deducted at cycle admission.
Batch mode scales payment with the native batch size; it does not change the
table. Gas must fit the native consumption rule, which takes one matching
stored fluid stack rather than combining several hatches. If there is too
little gas, none is consumed and the miner runs at its unboosted output.
Without a booster, all tiers retain their normal baseline production.
Other fluids are ignored as in GT. No tier-specific fluid error remains.

Native gas selection, ore boosts, cycle duration, energy use and tooltips are
unchanged. Startup auditing verifies the mixin on registered miners and the
native gas identities/order used by the table. Server fixtures exercise every
tier/gas pair, real powered output, exact/insufficient payment and batch costs,
including the largest payment. They assert gas preferences and scaling rather
than freezing every tuning value in a second table.

## Lightning Rod generation

All three native Lightning Rods (HV, EV and IV output) have a 10,000,000 EU
buffer and generate only during thunderstorms. Strike attempts remain every
256 ticks. The mast must contain at least 16 consecutive GT Iron Fence blocks,
with unobstructed air above it. Controller altitude provides no chance bonus.
The check includes the world's highest valid block layer, both for mast fences
and for obstructions above the mast.

The chance per attempt is 12.5% at 16 fences, increases linearly to 25% at 32,
and stays at 25% for longer masts. A successful strike fills the buffer and
destroys one random fence. Native 512 A output, voltage tiers, gradual buffer
loss and crafting recipes are retained. No spacing requirement is added.

Startup auditing verifies all three registrations, the generation hook, reduced
capacity and retained output ratings. Native server tests exercise weather,
mast validity, altitude independence, chance saturation and actual stored EU.

## Singleblock power failures

When a standard GT singleblock cannot draw the EU or steam needed to continue
a recipe, the current batch is discarded. Its consumed items and fluids are
lost, and its pending item and fluid outputs are discarded. Queued inputs,
reusable ingredients and previously completed output remain. The machine stays enabled
and can start another batch after power returns, subject to GT's normal recipe
checks. The rule also applies to addon machines using the same GT processing
tick; special machines with independent processing are outside this rule.

A missing machine certificate instead pauses an active batch, including when
the machine has no stored energy. After certification is restored, the batch
resumes if sufficient power is present; an actual failed power draw discards it.

## Singleblock transformer operation

All GT singleblock voltage transformers and buck converters have closed energy
input/output ports, at every tier and in either operating mode. This includes
normal, Hi-Amp and wet/advanced transformers. They reject unsided injection as
well, advertise no legacy energy demand/offer and cannot emit previously stored
EU or pull RF through their native input faces. Existing blocks and restored
saves obey the same rule. Saved energy is retained and there is no new NBT state.

Items, crafting and machine recipes remain available for use as ingredients.
Native Manual Transformer and TecTech Active Transformer remain operational.
BartWorks Energy Distributor only redistributes energy at the same voltage and
is excluded. IC2 transformers explicitly remain operational by user decision.
Ordinary generators and energy/dynamo hatches are unaffected. Startup audit
verifies the common port hook, transformer/converter registrations and Manual
Transformer exemption.

## Toxic Everglades machine ban

GT machines explode before processing in Toxic Everglades, including Steam
machines, generators, hatches and unformed controllers from GT's addons.
The check runs on the logical server at the start of the shared machine tick,
independently of energy, working state or tier. The native world provider
identifies the dimension, including a reassigned dimension ID.

Native explosion handling uses a fixed LV voltage (32 EU, native strength 3),
with its normal drops, pollution and global electrical-certificate revocation.
Machines with an empty explosion override, including TecTech multiblocks,
receive the same block removal, sound and terrain blast as a normal machine.
GT's terrain-explosion setting is respected; disabling terrain damage still
removes the banned machine. Other dimensions, non-GT machines, ordinary
structure blocks and hand mining are outside the rule.

Startup audit verifies the common tick hook and the registered provider.
Reduced dev without Everglades does not activate the dimension check.

## Chisel finishing for machine casings

- LV through UXV hand and Assembler production yields a raw machine casing.
  ULV through UMV require double plates; UXV uses a Dense MHDCSM plate and
  Double Magmatter plates. ULV still produces a finished casing directly.
- One metadata block contains fourteen raw variants. Each tier has an isolated
  native Chisel group with only its raw variant and the existing GT casing.
  Hand chisels and GT's Industrial Chisel use those groups, including NEI.
- MAX has its pair as a placeholder, without an invented production recipe.
  Raw casings have no finished-casing ore aliases; hulls and structures keep
  requiring the original finished blocks. Existing finished IDs are unchanged.
- Reduced dev instances without Chisel retain original recipe outputs. In the
  full pack, audits require the groups, all redirected routes and NEI discovery.

## Questbook crafting tasks

All BetterQuesting Standard crafting tasks also accept their required items
from the inventory without consuming them. Detection runs on inventory changes
or with Detect and uses each task's existing quantity, OreDictionary and NBT
matching rules. Observation Stations can detect items as for retrieval tasks.
Crafting, smelting, anvil use and crafting statistics retain their original
behavior where enabled by the task.

Production and inventory progress are tracked separately. For each required
item, the greater count applies instead of summing the two counts, and different
requirements may be satisfied by different methods. Inventory progress
follows native retrieval rules: partial counts are recalculated on detection,
while previously completed item requirements remain credited. The quest GUI
identifies both methods and shows that items are not consumed.

Questbook files and `bq_standard:crafting` task IDs remain unchanged. Existing
crafting progress and completed tasks are preserved; retrieval progress is saved
as additional task data. Existing quest descriptions are unchanged.

## Amplifier-only UU-Matter

- Singleblock Mass Fabricator rejects batches without sufficient UU-Amplifier
  before preparing output. Native paid processing consumes the amplifier.
- Matter Fabrication CPU loses both unamplified variants; paid variants stay.
- Fluid Extractor loses UU-Matter Berry and Space Flower UU-Matter extraction.
  UUA Berry and other amplifier production remain unchanged.
- The unamplified singleblock NEI recipe is removed as well. Audit verifies the
  native tables and the admission hook on every registered Mass Fabricator.

## Coolant Tower Ruthenium separation

Hot Ruthenium Tetroxide Solution separation moves from Distillation Tower
to Coolant Tower. The complete native recipe is copied: 9000 mB input,
7200 mB Ruthenium Tetroxide, 1800 mB Water and six Salt dust, at HV for 75 s.
All existing chances, metadata and input/output data are retained. Audits
require one addition, one removal and no remaining distillation bypass.

The tower exposes a standard GT recipe map and uses GT's ordinary NEI handler
and controller-catalyst index. Energy Hatches and an Output Bus fit its
existing hatch positions. Normal GT ProcessingLogic handles input admission,
progress and output, with mandatory item/fluid capacity protection. Missing
bus/power or blocked outputs reject the hot batch before consumption.
Running mapped recipes use TecTech's native energy drain with GT's long-power
field; no separate recipe engine or custom saved-state format is added.

Native passive Steam condensation remains free, at 160:1 on a 20-tick cycle,
without an Energy Hatch or Output Bus. Solar Salt changes remain deferred.

## LATEX cable transition

Assembler insulation-production recipes for GT cables rated EV or above are
removed across all widths and rubber/dielectric variants. Tier comes from
the registered insulated cable's rated voltage, independently of its width,
amperage or recipe EU/t. HV and lower cables, bare-wire production, existing
cables and downstream machine recipes remain unchanged. Every removed
output must have native Cable Coater/LATEX production before the transaction
can commit. Unexpected cable-to-cable Assembler conversions require review
instead of being silently removed. The audit records the actual final-table
removal count and verifies no matching Assembler output remains.

Earlier hand wrapping, Packager and Alloy Smelter routes remain intact.
LATEX retains its native structure, processing and controller crafts.

## Europium boule

All eight EBF gas variants of Europium boule consume two full Neptunium
Hexafluoride dust as an additional input. Silicon SG, Gallium Arsenide,
Europium, the circuit, each variant's gas and duration, energy, heat and
output are retained. Cutting into wafers remains unchanged. The startup
audit requires eight modifications.

## Dimensional Transcendent Plasma Forge coils

DTPF requires Hypogen or better coils. The check runs only at the end of
native structure validation, after all physical blocks, casing counts and
hatches are valid. Otherwise the controller shows only its original building
errors. A complete lower-coil structure shows:

> These coil blocks are too stinky! Use better ones.

The native controller error is translated/rendered on the client and uses
GT's existing structure-error synchronization. Revalidation clears it when
the structure becomes incomplete or receives acceptable coils. Controller
tooltips, construction hints and NEI retain the original coil information;
the new minimum is discovered through the complete structure. Per-recipe
heat requirements and all processing mechanics are retained. Startup checks
verify the hook and the default error localization.

## Mutated Living Solder

The Chemical Plant recipe consumes eight Nuclear Stars instead of eight
Gravistars. Biocells, Infinity Catalyst dust, the Infinite Mutation Catalyst,
all fluids, 40320 mB solder output, 800 s duration, 3842160 EU/t and casing
tier 7 are retained. The independent QFT route is unchanged. Native
processing can produce the initial Nuclear Star at 4 A UHV, before a UEV
Energy Hatch; no earlier star recipe or power shortcut is added. The audit
requires one Chemical Plant modification.

## Raw Bio Catalyst Medium

All three Bacterial Vat variants producing Raw Bio Catalyst Medium consume
one full Radioactive Mineral Mix dust as an additional item input. Existing
ingredients, cultures, glass and radiation requirements, time, voltage and
native fluid amplification are retained. The item cost is per recipe run;
fluid amplification does not multiply it. The startup audit requires three
modified recipes. Independent Mineral Mix production routes are retained.

## PCB Factory wetware and bioware

The twelve Wetware Extreme and nine Bioware Ultra board variants in PCB
Factory consume Copper Plasma instead of Sulfuric Acid, in the same amount
of mB. The other fluids, plastic tier, circuit, board yield, nanite material,
factory tier, upgrade, time and energy are retained. Optical and lower boards
are unchanged. Base board production in Circuit Assembler and chemical
finishing in Chemical Reactor/LCR remain available without Copper Plasma.
No earlier Copper-plasma recipe is added. The audit requires all 21 changes.

## Americium-doped boule

The EBF accepts only the existing Oganesson variant for Americium-doped
boule. It also consumes two full Protactinium dust. The seven other gas
variants are removed. Silicon SG, Gallium Arsenide, Americium, circuit,
gas amount, duration, energy, heat and output are retained. Cutting the
finished boule into wafers is unchanged. One modification and seven
removals are required by the startup audit.

## High-tier hull insulation

UV and UHV hulls require Kevlar insulation: 288 mB molten Kevlar in the
Assembler or two Kevlar plates in hand crafting. UEV through UXV hulls
require 576 mB molten Radox Polymer in the Assembler. Their existing hand
recipes use Radox Polymer in the original form: two double plates on
UEV/UIV/UMV, two dense plates on UXV. Cables, central metal plates, casings,
time, voltage and yield are retained. Lower-tier hulls are unchanged.

All six Assembler and six hand recipes are changed together; the audit
also rejects additional hand-production alternatives. Native hull recycling
continues to recover only casing and cable materials, excluding the extra
assembly insulation to prevent duplication.

## Naquadah purification water

Conversion of Naquadahine to pure Naquadah consumes 1000 mB Grade 1 purified
water per batch in the EBF, Chemical Reactor and LCR. Enriched Naquadah Sulphate
conversion consumes 1000 mB Grade 2 water in the EBF and both Electrolyzer maps;
Naquadria Sulphate consumes 1000 mB Grade 3 in those same routes. All nine edits
retain metal yield, byproducts, processing time, voltage and heat.

Smelting already pure metal dust, cooling hot ingots, casting and the separate
Earth/QFT routes retain their existing behavior. Grade 1/2 require the existing
LuV water installation; Grade 3 requires the ZPM Flocculation unit. The original
EBF recipe voltages are retained. Native recipe indexes are rebuilt by the
existing transaction system, and startup audits require all nine changes.

## Legacy fusion structures

Legacy Fusion MK2-MK5 require Electromagnetically-isolated Casing from BEC
in their coil positions. Ordinary reactor casings and hatches are retained;
MK1 and all Compact structures keep their existing coils. Controller recipes
remain available for Compact crafting. Native validation, construction and
structure hints use the replacement, and controller tooltips name it.
Startup audits verify the four changed prototypes and the unchanged reactors.

## Ozonator safety

The Ozonator explodes at cycle admission if an ozone input hatch contains
strictly more than 1000000 mB. Its highest variant consumes 1000000 mB and
keeps the native 80% base chance. Lower variants and hatch capacities are
unchanged; a full ZPM hatch contains 1024000 mB and is therefore unsafe.
The native threshold/variant tooltip is updated. Explosions snapshot the
ozonator's hatch list and skip tiles already destroyed by the blast, avoiding
native removal-callback exceptions. The existing global electrical
certification consequence remains active.

## Flocculation waste

Flocculation Waste Liquid cannot be distilled back into Aluminium, Oxygen
and Hydrochloric Acid. The recovery recipe is removed and startup checks
require no remaining waste-input route in Distillation Tower or Distillery.
Flocculation still produces the waste, which can be stored, transported or
disposed of as before.

## Electricity Gate

Basic electric GregTech machines from LV upwards require global electrical safety certification.
Uncertified basic machines pause processing and show a clickable certificate-warning
icon, linking to the missing certificate's NEI information page. Basic Bronze/Steel
Steam machines and Steam multiblock controllers require the separate High Pressure
Operator Certificate. Steam multiblocks pause their active batch until the
certificate is restored, including the Steam Water Pump; their controllers do
not currently show a certificate-warning icon. LV and
higher basic electric machines require both. From MV onwards they also require
the paid certificates up to their tier, from MV through MAX. Energy and dynamo
hatches from LV upwards require the same prerequisite chain, including multi-amp,
laser and wireless variants. ULV electric machines, generators, boilers and other
multiblock controllers are outside the direct processing gate. The unpowered Ice
Cream Machine is also exempt from certification. Right-clicking the certificate
unlocks gated machines for all players and dimensions in the save, consuming it
once in survival. Its NEI page
opens a drawing-based ABCD examination in ModularUI 2. Passing at least eight
of the ten preliminary questions awards the item.
Admission costs 1000 GT Bronze ingots for each Steam attempt and 1000 single Tin
cables for each electrical attempt, with no refund for failure or closing
the paper. MV requires 556 Steel blocks (5004 ingots) and HV requires 1112
Aluminium blocks (10008 ingots). EV requires 1112 Stainless Steel blocks and
IV requires 1112 Titanium blocks (10008 ingots each). These tiers have no exam;
payment certifies the world immediately. Earlier certification revocations never
erase a later earned certification, but dependent machines still pause.
LuV costs 670 finished TungTungTungstensteel blocks (6030 ingots). The later
placeholder fees are 1112 blocks (10008 ingots) each: ZPM uses Rhodium-Plated
Palladium, UV Iridium, UHV Osmium, UEV Neutronium, UIV Infinity, UMV Transcendent
Metal, UXV Space-Time and MAX MHDCSM. These tiers also certify the world directly.
The item is hidden from NEI item search. Players discover the restriction
through the machine's error icon or a hatch's clickable chat warning on placement
and empty-hand right-click. Locked hatch buffers are frozen and become available
again after recertification. Multiblock OC and parallel limits remain native.
See [Progression certificates](PROGRESSION.md)
and [electrical exam](CERTIFICATION_EXAM.md) for the full contract.

Native GT cable fires and machine explosions revoke the certification for the
entire save. Gated processing pauses on the next tick and resumes only after
using a newly issued certificate. Spare certificates earned before revocation
expire, so recertification requires passing the exam again. The revocation and
certificate issue epoch persist in world data; players receive a global notice.
Actual player heat/frost damage from a GT fluid pipe (contact or leak), or damage
from a Steam machine exhaust, revokes Steam certification globally. Protected
players and mobs do not trigger it. Steam and electric basic machines pause until
Steam is renewed; the electrical flag is retained. The Steam drawing exam uses
the supplied High Pressure Operator paper with an 8/10 passing threshold.

## Brainrot materials and prototype Brainrotter

GregTech's existing Tungstensteel is displayed as **TungTungTungstensteel**,
including its processed forms. Existing item identities and recipes are preserved.
A separate **Tungstensteel** material (`RawTungstenSteel`) is registered for the
Brainrotter input, with separate items and molten fluid. The prototype Industrial
Brainrotter uses the supplied 5×5×3 glass and fireproof Forestry log structure,
with GT hatches in its wooden bottom layer and EV power. The Thaumcraft pedestal
must be empty before a recipe starts. A single raw ingot is displayed during
processing; removing or replacing its cycle marker aborts the entire pending
output. The marker is removed on completion or cancellation and saved with the
active recipe. Foreign pedestal items are preserved.
It refines cooled raw ingots into finished ingots and raw molten into finished molten,
with a reusable generic Brainrot Compilation in an input bus and consumed Wood
Pulp directing the transformation (one portion per finished ingot). Hot ingots
must first be cooled in a Vacuum Freezer, with the same duration and energy cost as the pack's
finished-material cooling recipe. Both regular and multiblock Steel + Tungsten
mixers now produce raw dust. The two Quantum Force ore-processing routes also
produce raw Tungstensteel, preserving their other products and chances;
all EBF gas variants and FoTG's derived molten route produce raw material for
Brainrotter refinement. Finished-stock recycling remains available. The finished
material displays the supplied Sahur monologue as its chemical formula while
retaining its actual composition. See [Brainrot materials](BRAINROT_MATERIALS.md) for recipes,
construction and the staged plan.

## Lootbags

Extra Nerf modifies EnhancedLootBags 1.3.5's native tables in memory after loading
XML, before publication. ELB continues to handle random selection, Fortune, native
player limits and opening. The XML files are not rewritten.

**ME Controller:** remains in AE Basic (37) and AE Good (38), including the
inherited Basic pool. Basic stays at one unit; Good changes from four to one.
Selection weights are unchanged. A server-wide allowance permits at most
one controller from lootbags per save, across players, dimensions, Fortune and
restarts. There is no guaranteed first drop. Crafting, other reward systems and
existing inventories are not affected.

Both server and client apply the quantity patch to their native tables. NEI keeps
the controller listed and shows the last allowance received from the server, or
an unknown state if none is available. Once exhausted, it displays a 0% effective
chance. An available allowance is not a guarantee: ELB's random draws and separate
player limits still apply. The displayed snapshot can become stale if another
player claims the reward concurrently. Controller tooltips replace the native
base percentages and player counter; other lootbag percentages are identified as
single-draw weights before limits and retries, with a warning that native player
counters may be stale. Use matching client/server XML:
ELB does not synchronize full drop lists or arbitrary server-only file edits.

Changes are verified by the startup audit and each `/lootbags reload` (alias
`/lbag reload`). Missing/changed expected controller entries fail validation.
ELB invalidates its merged caches after a successful reload. Inline addloot,
addinventory and addgroup commands are blocked; edit XML and reload instead.

Only globally limited rewards create claims in the world's
`data/gtnhextranerf/loot/claims.journal`. A failed or uncertain delivery does not
refund the allowance. Native per-player counts remain managed by ELB/YAMCore.
See [architecture and limits](LOOTBAG_SYSTEM_DESIGN.md).

## Crafting Input Proxy

The Space Assembler recipe uses one UQV Sensor instead of one UEV Sensor.
UQV components have no production recipes and are hidden from NEI item search;
the Proxy recipe remains visible and shows the UQV Sensor as its ingredient.
Other ingredients, fluids, time, voltage and required module tier are preserved.
See [`COMPONENTS.md`](COMPONENTS.md) for the component registry and appearance.

## Power generation and steam

### Steam Input Hatch

The Steam Input Hatch holds 640,000 L instead of 64,000 L. The larger buffer
reduces power failures on steam multiblocks with high per-tick demand.

### Steam Turbine

- Efficiency is 20% at LV, 30% at MV and 40% at HV. Each tier above HV
  would add 5 percentage points.
- The machine description shows the new efficiency and computed maximum steam
  consumption.
- Examples: LV 20%, MV 30%, HV 40%.

### Diesel Generator

- Efficiency is 20% at LV, 30% at MV and 40% at HV. Each tier above HV adds
  5 percentage points.

### Gas Turbine

- Efficiency is 20% at LV, 30% at MV and 40% at HV. It then rises by
  5 percentage points per tier, reaching 45% at EV and 50% at IV.

### Plasma Generator

- Its progression restarts at its first available tier. Efficiency is 20% at
  EV, 30% at IV, 40% at LuV, 45% at ZPM and 50% at UV.

### Semi-Fluid Generator

- GT++ Semi-Fluid Generator efficiency is 20% at LV, 30% at MV and 40% at HV.
  It then rises by 5 percentage points per tier, reaching 45% at EV and 50%
  at IV.

### Steam conversion

- Single-block steam machines use five times the vanilla amount: 10 L per EU
  instead of 2 L. NEI, WAILA, fluid tank information and the ModularUI 2
  steam gauge use the same conversion. The internal energy buffer keeps its
  size; a full Bronze machine's 16,000 internal units now represent 160,000 L.
  The legacy ModularUI 1 gauge still uses GregTech's original display ratio.
- Steam processing multiblocks use five times their native steam consumption,
  after GregTech's efficiency adjustment. Their WAILA usage follows the change.
- Multiblock NEI pages use GregTech's generic recipe description and may not
  reflect the formed machine's tier, parallelism and efficiency; WAILA reports
  the running machine's usage.
- The Steam Water Pump's 1,500 L cost per cycle and boiler production are not
  changed. A heated Small Coal Boiler produces 120 L in 20 ticks when supplied.

### Benzene and Phenol

- Benzene fuel value is divided by four: the expected 360 becomes 90.
- Phenol fuel value is divided by four: the expected 288 becomes 72.

### Configuration circuits

- Every tiered machine with configuration-circuit support exposes a physical
  Integrated Circuit slot below HV instead of the built-in ghost selector.
- The physical slot accepts exactly one Integrated Circuit. The circuit must
  match the recipe configuration but is not consumed.
- A physical circuit is returned with its NBT when its machine or input bus
  is dismantled, and follows the normal inventory-drop rules during explosions.
- HV and higher tiers keep their existing ghost selector behavior.
- Saved zero-size phantom circuits are cleared server-side when a gated
  machine first opens its GUI and creates the physical slot. A real Integrated
  Circuit must then be inserted.

## Large Chemical Reactor behavior

- The Large Chemical Reactor uses 3/4 overclocking: speed factor 3.0 and energy
  factor 4.0.
- Its tooltip replaces the perfect-overclock line with
  `Now with amazing 3/4 OC!`.

## Nuclear items

### Glowstone breeder rod

- The Glowstone/Sunnarium breeder cell requires 1,000 pulses.

### Radioactive fuel-rod lifetime

The maximum durability/lifetime is divided by ten for item names containing:

- Thorium;
- Uranium;
- MOX or Mox.

Energy per operation, heat, radiation and depleted outputs are not changed by
this patch. Lithium and unrecognized cell families are untouched.

## Ore generation

### Naquadah small ore

- The Naquadah small-ore vein amount is forced to 1.

## Crafting changes

### Large Chemical Reactor

Both the crafting-table and assembler routes now require EV circuits instead
of HV circuits.

Crafting-table recipe:

```text
Circuit  Rotor  Circuit
PTFE     Motor  PTFE
Circuit  Hull   Circuit
```

- circuits: EV;
- rotor: Stainless Steel;
- pipes: large PTFE;
- motor and hull: HV.

The recipe is explicitly non-mirrored.

### Flint and Steel

When the Dreamcraft recipe is present:

- Small Steel gear becomes a full Steel gear;
- Small Steel spring becomes a full Steel spring.

### TConstruct Tool Station Slab

The shapeless Tool Station Slab recipe requires:

- a crafting saw;
- a TConstruct Tool Station;
- any LV circuit registered as `circuitBasic`.

### LV Circuit Assembler

- The two MV circuits in the shaped crafting recipe become LV circuits.
- Both slots accept any `circuitBasic` variant, including mixed variants; no
  conversion into a universal circuit item is required.

### Good Circuit

- The shaped hand-crafting recipe is removed.
- Good Circuits remain available through the Circuit Assembler recipe
  described below.

### Bronze dust

- The shapeless `3 Copper dust + 1 Tin dust` route produces 1 Bronze dust
  instead of 3.

### LV Electric Motor

- The Iron Magnetic/Any Iron variant is removed from crafting and the Assembler.
- The Steel Magnetic/Steel variant remains and becomes dismantleable.

### MV Electric Motor

The hand-crafting and Assembler recipes require a Magicnetized Manasteel Rod
instead of a Magnetic Steel Rod. In an Arcane Workbench, place a Manasteel Rod
between two Magnetic Steel Rods with a Solegnolia below it. The recipe costs
10 Aer and 10 Ordo vis, keeps the Solegnolia in the workbench, and turns the
Magnetic Steel Rods into Steel Rods.

An HV Assembler can also produce one Magicnetized Manasteel Rod from one
Manasteel Rod, two Magnetic Steel Rods, two Salis Mundus and 144 L Mercury.
Solegnolia acts as a reusable catalyst. The recipe takes 20 seconds at 480 EU/t.
The two Magnetic Steel Rods return as ordinary Steel Rods.

The Component Assembly Line produces MV motors with Long Magnetic Steel Rods.

### HV Electric Motor

The crafting and Assembler recipes require a Magicnetized Void Metal Rod.
An MV Assembler produces it in 30 seconds at 120 EU/t using:

| Input | Amount |
| --- | ---: |
| Void Metal Rod | 1 |
| Magicnetized Manasteel Rod | 2 |
| Magnetic Steel Foil | 4 |
| Salis Mundus | 2 |
| Mercury | 144 L |
| Solegnolia, reusable catalyst | 1 |

The recipe returns two ordinary Manasteel Rods for another magicnetizing cycle.
GregTech Assembling Machines have two output slots to deliver both products.


### Rings

- GregTech ring crafting requires a long stick instead of a normal stick.
- BartWorks/Workstoff ring crafting receives the same normal-stick to
  long-stick change for each supported material.

### Extreme Heat Exchanger

The existing crafting recipe is replaced with:

```text
E P E
P H P
S P S
```

- `P`: Tritanium plate;
- `H`: ZPM machine hull;
- `S`: MAR-Ce-M200 plate;
- `E`: IC2 Diamond Reactor Heat Switch.

The replacement is reversible.

### Slime processing and EMT Glue

Slime is no longer a source of Extractor or Centrifuge products:

- the vanilla Slimeball Extractor recipe is removed;
- when TConstruct is present, Gelatinous Slime and the two slime-block
  variants are also removed from the Extractor;
- the TConstruct recipe extracting Raw Rubber from 16 Slimy Leaves is removed;
- when TConstruct is present, all six slime-family Centrifuge recipes are
  removed: the vanilla Slimeball, Gelatinous Slime, all three TConstruct slime
  colors, and Slime Mud;
- when GT++ is also present, the same six recipes are removed from the
  multiblock Centrifuge map.

Selected crafting shortcuts no longer accept any slimeball variant. They are
replaced by recipes using Electro-Magic Tools Glue (`EMT:EMTItems:8`):

- Sticky Piston: 1 Glue in both the crafting-table and Assembler recipes;
- one Coated Circuit Board: 1 Wood plate and 2 Glue;
- three Coated Circuit Boards: 3 Wood plates and 6 Glue;
- one Resistor: the original Copper wires and Carbon-family dust plus 2 Glue.

The four Resistor variants for Coal, Carbon, Charcoal and Lignite remain.
Existing Sticky Resin recipes and the pre-existing fluid-glue Assembler route
are not changed.

## Cable assembly

Five GregTech cable assembler families receive an additional
Polyphenylene Sulfide (PPS) foil input:

- Rubber insulation;
- Styrene-Butadiene Rubber insulation;
- Silicone insulation;
- four-cable dielectric recipes;
- single-cable dielectric recipes.

MV cables made from Iron, Nickel, Cupronickel, Copper, or Annealed Copper and
HV cables made from Kanthal, Gold, Electrum, Silver, or Blue Alloy do not
receive PPS foil in any of these families, at any cable size.

The foil quantity scales with the amount of insulation/cable material in the
upstream recipe. The existing integrated-circuit item and all other inputs are
preserved.

## Steel production

### Primitive Blast Furnace fuels

Among Primitive Blast Furnace recipes that produce Steel, only recipes using
one of the following fuel families remain:

- an ore-dictionary `fuelCoke` input;
- a Railcraft Coal Coke Block.

In the supported full pack this retains 7 Steel recipes and removes the other
39. Primitive Blast Furnace recipes that do not produce Steel are outside this
patch and remain untouched.

### Electric Blast Furnace Steel routes

The following 25-second MV, 1,000 K route is removed:

- 1 Iron dust;
- 1 Any Carbon dust;
- integrated circuit 11;
- 1,000 L Oxygen;
- output: 1 Steel ingot, 1 Ash dust and 250 L Carbon Dioxide.

The Cast Iron route is changed to:

- 3 Cast Iron dust;
- integrated circuit 11;
- 6,000 L Oxygen;
- output: 3 Steel ingots, 1 Ash dust and 2,500 L Carbon Dioxide;
- 45 seconds at MV and 1,000 K.

A new Cast Iron production route is added:

- 3 Iron dust;
- integrated circuit 1;
- 2 Any Carbon dust;
- output: 3 Cast Iron ingots;
- 20 seconds at MV and 1,000 K.

The bulk EV route remains unchanged: 50 Iron dust, one Any Carbon dust and
circuit 2 produce 50 Steel ingots and one Ash dust in 150 seconds at 2,000 K.

The Alloy Blast Smelter's old Iron-and-Coal route is also removed. Its Cast
Iron route requires 3 dust and 6,000 L Oxygen, producing 432 L molten Steel in
36 seconds at MV. As with GT++'s generated recipes, it produces the molten
metal rather than the EBF's item and gas byproducts.

## Aluminium production

### Direct Aluminium Oxide + Carbon route

The blast-furnace recipe consuming 10 Aluminium Oxide dust and 3 Carbon dust
to produce 4 Aluminium ingots and 3,000 L Carbon Dioxide is removed.

### Cryolite route

- Duration increases from 80 seconds to 120 seconds.
- Inputs, four-ingot output, MV voltage and 963 K requirement remain.

### Aluminium dust routes

Nitrogen route:

- consumes 1,000 L Nitrogen and circuit 11 as before;
- duration increases from 60 to 90 seconds;
- HV voltage and 2,054 K remain.

No-gas route:

- remains available with circuit 1;
- duration increases from 75 to 140 seconds;
- HV voltage and 2,054 K remain.

Seven alternative gas routes are removed:

- Helium;
- Argon;
- Radon;
- Neon;
- Krypton;
- Xenon;
- Oganesson.

### TConstruct aluminium

When TConstruct is present:

- Nitrogen route duration increases from 60 to 90 seconds at MV and 1,300 K;
- the no-gas route remains, but duration increases from 75 to 120 seconds and
  heat rises from 1,300 to 1,600 K;
- the seven alternative noble-gas routes listed above are removed.

The corresponding Alloy Blast Smelter routes follow the same gas restrictions.
Their base duration is 80% of the corresponding EBF duration: 72 seconds with
Nitrogen, 112 seconds without gas for Aluminium dust and 96 seconds without
gas for TConstruct aluminium.

## Ilmenite, Rutile and Ruby processing

### Removed Ilmenite chemical baths

Both Sulfuric Acid chemical-bath routes are removed:

- crushed Ilmenite;
- purified crushed Ilmenite.

The removed recipes produced Rutile, probabilistic Ilmenite Slag and Green
Vitriol.

### Ilmenite blast furnace

| Value | Before | Now |
| --- | ---: | ---: |
| Ilmenite dust input | 5 | 10 |
| Carbon dust input | 1 | 2 |
| Wrought Iron ingot output | 1 | 2 |
| Rutile dust output | 3 | 6 |
| Ilmenite Slag output | none | 1 |
| Carbon Monoxide output | 1,000 L | 2,000 L |
| Duration | 160 s | 300 s |

The recipe remains HV at 1,700 K.

### Ilmenite Slag centrifuge

- Duration increases from 2 to 10 seconds.
- Existing Iron, Niobium, Tantalum, Manganese and Magnesium outputs keep their
  original chances.
- The same changes apply to the Industrial Centrifuge.

### Removed Ruby Juice chain

Eight Ruby Juice mixer recipes are removed. The affected Ruby forms are:

- crushed;
- purified crushed;
- impure dust;
- pure dust.

For every form, both the one-unit/1,000 L and nine-unit/9,000 L recipe are
removed.

The Ruby Juice centrifuge recipe that returned Aluminium Hydroxide, Chrome,
Iron, Vanadium, Magnesium and Hydrochloric Acid is also removed. Both the
ordinary and industrial mixer and centrifuge recipe maps are covered.

## Titanium

### Titanium blast furnace

- Magnesium Chloride output is reduced from 6 dust to 4 dust.
- Inputs remain 2 Magnesium dust, circuit 12 and 1,000 L Titanium
  Tetrachloride.
- The hot Titanium ingot, 40-second duration, HV voltage and 2,140 K remain.

## Indium and Silicon chemistry

### Indium concentrate

In both the single-block Chemical Reactor and Large Chemical Reactor:

- Aluminium dust input increases from 4 to 8;
- circuit 1, 8,000 L concentrate, tiny Indium output and 8,000 L Lead-Zinc
  Solution remain unchanged.

### Silicon from Sodium

In both chemical-reactor maps:

- Salt dust output is reduced from 8 to 7;
- Silicon SG output remains one dust.

### Silicon from Hydrogen/Trichlorosilane

Hydrochloric Acid output is reduced from 3,000 L to 2,000 L in three routes:

- Hydrogen cells in the single-block reactor;
- gaseous Hydrogen in the Large Chemical Reactor;
- a Trichlorosilane cell plus gaseous Hydrogen in the single-block reactor.

### BartWorks Zinc route

In both chemical-reactor maps:

- the secondary BartWorks dust output is reduced from 6 to 5;
- Silicon SG output remains one dust.

### Cutter coolants

Cutting Machine recipes do not accept ordinary Water or Distilled Water.
Lubricant and Dimensionally Shifted Superfluid remain available. Special
recipes using other fluids, including purified water grades and UU-Matter,
are unchanged. The industrial Cutting Machine uses the same recipes.

### LV Lubricant distillation

For integrated circuit 24 Distillery recipes, oil input doubles from 12 L to
24 L while output, duration and voltage remain unchanged:

| Oil | Lubricant output |
| --- | ---: |
| Light Oil | 3 L |
| Medium Oil | 6 L |
| Heavy Oil | 9 L |

## Plutonium decay

### Circuit 8 route

- 64 Plutonium ingots plus one Uranium dust produce 63 Plutonium dust instead
  of 64.
- The recipe still consumes 8,000 L Air and produces 800 L Radon in 75 seconds
  at HV.

### Circuit 10 Helium route

A Large Chemical Reactor route is added:

- 64 Plutonium ingots;
- 1 Uranium dust;
- circuit 10;
- 8,000 L Air;
- 800 L Helium;
- output: 63 Plutonium dust and 960 L Radon;
- 75 seconds at HV.

## Endstone, Staballoy and Tellurium

### Endstone centrifuge

- Tungstate chance is reduced from 37.5% to 10%.
- Helium output is reduced from 4,320 L to 1,500 L.
- Platinum Metallic Powder remains at 25%.
- Sand remains at 90%.
- The Industrial Centrifuge uses the same recipe changes.

### Staballoy centrifuging

Separating 10 Staballoy dust into 9 Uranium-238 dust and 1 Titanium dust requires
**IV (7,680 EU/t)** instead of HV (480 EU/t). This applies to both the ordinary
centrifuge and the industrial centrifuge recipe map. The base duration remains
48 seconds, with the same inputs, outputs and guaranteed yields.

### Tellurium from crushed Lead

The four-second MV blast-furnace recipe sets:

- Lead ingot chance to 100%;
- two Tellurium nuggets chance to 0.1%.

## Europium and Americium fusion

The molten Europium and Americium routes use the following balance. Quantities below
are mB; times are base recipe times, before machine overclocking.

| Route | Inputs | Output | Time |
| --- | --- | --- | --- |
| Europium, base | 64 Neodymium + 192 Hydrogen gas | 4 Europium | 32 ticks (1.6 s) |
| Europium, Promethium | 16 Neodymium + 8 Promethium | 16 Europium | 32 ticks (1.6 s) |
| Americium, base | 64 Lutetium + 64 Chrome | 4 Americium | 96 ticks (4.8 s) |
| Americium, Promethium | 16 Lutetium + 8 Promethium | 16 Americium | 96 ticks (4.8 s) |

The base routes use four times their original inputs and produce one quarter
of the original output: **16 times the material cost per unit of product**.
The Promethium routes halve both original inputs and output, and quadruple
their duration, matching the original base routes' batch output and duration.
EU/t and ignition thresholds remain unchanged: Europium uses 24,576 EU/t and
150 million EU ignition; Americium uses 49,152 EU/t for the base route and
38,764 EU/t for Promethium, both with 200 million EU ignition.

### Americium plasma ignition

The fusion recipe using 144 mB molten Plutonium-241 and 2,000 mB Hydrogen gas
to produce 144 mB Americium plasma requires **1,000,000,000 EU ignition (MK4)**,
up from 500,000,000 EU (MK3). Its inputs, output, base duration of 64 ticks
(3.2 seconds), and 98,304 EU/t running cost remain unchanged.

## Platinum processing

### Refined Platinum Salt

- The blast-furnace route no longer outputs 87 L Chlorine.
- Platinum Metallic Powder output remains.

### Raw Platinum Powder

In both chemical-reactor maps:

- Calcium Chloride output is reduced from 3 dust to 2;
- Platinum output remains 2 dust.

## Samarium processing

The listed concentrate and blend changes apply to both ordinary and industrial
centrifuges or electrolyzers, respectively.

### Dephosphated Samarium Concentrate

- Samarium dust chance is reduced from 90% to 5%.
- Thorianite chance remains 80%.

### Small Samarium blend

Output chances become:

- Samarium: 10%;
- Sodium: 100%;
- rare-earth residue: 100%.

### Large Samarium blend

Output chances become:

- Samarium: 10%;
- Sodium: 100%;
- rare-earth residue: 100%.

## Circuits and circuit boards

The separate Circuit Assembly Line may retain cheaper circuit recipes than the
changed Circuit Assembler recipes. This is intentional.

### Integrated Logic Circuit

- ILC Chip input increases from 1 to 4.

### Good Circuit

- Basic Electronic Circuit input increases from 2 to 4.

### Advanced Circuit

In both the standard and ASMD Circuit Assembler recipes:

- ILC Chip input increases from 2 to 8;
- RAM Chip input increases from 2 to 8.

### Nano and Quantum processors

All non-null item inputs are multiplied by six, capped at 64 per input stack,
for four Circuit Assembler recipes:

- Nanoprocessor standard variant;
- Nanoprocessor advanced-component variant;
- Quantumprocessor standard variant;
- Quantumprocessor advanced-component variant.

For example, 8 items become 48, while 12 or 16 items become 64. Zero-sized
non-consumable inputs remain zero.

### Elite circuits

The active solder-based standard and ASMD recipes are removed and replaced by
cleanroom recipes requiring 100 L Soul Infused Medium.

Standard recipe:

- 2 Aluminium frames;
- 2 Data circuits;
- 12 coils;
- 24 capacitors;
- 16 RAM chips;
- 24 Annealed Copper 1x wires;
- 80 seconds at HV.

ASMD recipe:

- 2 Aluminium frames;
- 2 Data circuits;
- 3 ASMD inductors;
- 4 ASMD capacitors;
- 16 RAM chips;
- 16 Annealed Copper 1x wires;
- 40 seconds at HV.

Each produces one Elite circuit.

### PPIC wafer

In both chemical-reactor maps:

- NPIC wafer input increases from 1 to 64;
- 64 Indium Gallium Phosphide dust and 1,440 L molten Sunnarium remain.

### Removed diode assembler routes

Four LV assembler recipes are removed:

- Gallium Arsenide + GregTech molten Glass -> 2 diodes;
- Gallium Arsenide + registry molten Glass -> 2 diodes;
- Silicon Wafer + GregTech molten Glass -> 1 diode;
- Silicon Wafer + registry molten Glass -> 1 diode.

Each also used 4 fine Copper wires and integrated circuit 4.

## Machine casings and components

### Heating coils

Heating coils from Cupronickel through Awakened Draconium require eight 8x
wires instead of eight 2x wires and twice as much insulation.

| Coil | Insulation per block |
| --- | --- |
| Cupronickel | 24 Alumino Silicate Wool or 16 Mica Insulator Foils |
| Kanthal | 24 Mica Insulator Foils |
| Nichrome | 32 Mica Insulator Foils |
| TPV-Alloy | 40 Mica Insulator Foils |
| HSS-G | 48 Mica Insulator Foils |
| HSS-S | 56 Mica Insulator Foils |
| Naquadah | 64 Mica Insulator Foils |
| Naquadah Alloy | 72 Mica Insulator Foils |
| Trinium | 16 Naquarite Universal Insulator Foils |
| Fluxed Electrum | 24 Naquarite Universal Insulator Foils |
| Awakened Draconium | 32 Naquarite Universal Insulator Foils |

### MV Energy Hatch

- ULPIC Chip input increases from 2 to 4.
- The hand-crafting recipe (two ULPIC Chips, two MV Coils, two Lubricant Cells,
  Copper cable, MV Pump and MV Hull) is removed. Its grid is full, so the extra
  chips cannot be added, and the Assembler is the only way to make it.

### Machine casings

Both Assembler and hand-crafting recipes require 8 double plates instead of
8 normal plates for:

| Tier | Material |
| --- | --- |
| ULV | Cast Iron |
| LV | Steel |
| MV | Aluminium |
| HV | Stainless Steel |
| EV | Titanium |
| IV | TungTungTungstensteel |
| LuV | Rhodium-Plated Palladium |
| ZPM | Iridium |
| UV | Osmium |
| UHV | Neutronium |
| UEV | Infinity |
| UIV | Transcendent Metal |
| UMV | SpaceTime |

Output is one casing of the corresponding tier: raw from LV upward, finished
at ULV. Native Chisel supplies the finishing step described above. Hand
crafting retains the wrench in the center of the plate ring.

UXV instead requires **one Dense MHDCSM Plate and four Double Magmatter
Plates** in both hand crafting and the Assembler. The Assembler retains
integrated circuit 8. In hand crafting, `M` is Double Magmatter, `D` is Dense
MHDCSM, `W` is a wrench, and `.` marks an empty position:

```text
MDM
.W.
M.M
```

### Diamond Sawblade

- The assembler gear input changes from Cobalt Brass to Blood Steel.
- The hand-crafting recipe takes a Blood Steel Gear instead of a Cobalt Brass
  Gear, with the same four Small Piles of Diamond Dust.

### Superconductor Coil Casing

- LuV variant: 64 2x superconductor wires become 64 8x wires; Mica Insulator
  Foil increases from 48 to 192, matching the fourfold increase in wire thickness.
  This replaces the previous 128-wire / 384-foil nerf.
- ZPM variant: 32 2x superconductor wires become 32 4x wires; Naquarite Universal
  Insulator Foil increases from 4 to 8, matching the doubled wire thickness.
- Both still produce one Superconducting Coil Block in 50 seconds at 9,001 EU/t.
  Molten Trinium remains 1,440 L for LuV and 576 L for ZPM.

## Endgame and space materials

### Dense Netherite plates

- The targeted Assembly Line recipe requests 64 dense Netherite plates.
- The current semantic target is the Netherite request in GregTech's assembly
  line recipe loader.

### Raw Neutronium

- Helium plasma input increases from 144 L to 1,440 L.
- The Industrial Mixer also requires the increased amount of plasma.

### TPV Alloy

- TPV Alloy dust mixing yields three dust instead of seven in both the normal
  and multiblock mixer maps. Inputs, circuit setting, processing time and EU/t
  remain unchanged.

### AE2

- ME P2P tunnels carry at most eight channels through their external nodes
  when AE2 channels are enabled. Multiple outputs share the input's eight-channel
  capacity. Ordinary Dense Cable retains its 32-channel capacity.

- Wireless Connector links cost `min(distance^4, 10,000,000)` EU/t in total, split equally
  between endpoints. Distance is Euclidean between block coordinates, so a
  one-block wall between connectors gives a distance of two blocks and costs
  16 EU/t. A 32-block link costs 1,048,576 EU/t.
- Each endpoint charges at most 5,000,000 EU/t per link. The cap applies per
  connection; a hub or network with multiple links can consume more in total.
- Wireless Hubs sum their half of each connected link. Removing a link removes
  its power cost; an unconnected endpoint has no connection power draw.
- This replaces AE2's connector base/distance pricing. AE unit conversion and
  the configured power multiplier are accounted for to express the rule in EU/t.

- The previous Charged Certus Quartz nerf is removed; chemical reactors retain
  their upstream Redstone recipes.
- Titanium in recipes producing AE2 items becomes TPV in the same form and
  quantity, including screws and molten metal. Other materials and alloys remain.
- ME Controller crafting and Assembler recipes are removed. Its Assembly Line
  recipe takes 30 seconds at LuV and requires research from scanning a 64k ME
  Storage Cell for 60 seconds at HV. Scanning consumes the cell, including any contents.
- Blank Pattern uses three Sunnarium plates instead of three Glowstone plates.

See [AE2 progression and Controller recipe](AE2_PROGRESSION.md).

### Quantum Star

- Radon requirement for the Nether Star chemical-bath route increases from
  1,250 L to 5,000 L.

## Forestry-dependent Indium and vacuum-furnace changes

These changes run only when Forestry is loaded.

### Indium comb chemical processing

- Concentrate route: Indium output decreases from 3 dust to 2.
- Indium Phosphate route: Indium output decreases from 6 dust to 4.
- Purified crushed route: output decreases from 12 to 8 in both
  chemical-reactor maps.
- Roquesite route: Indium output decreases from 3 dust to 2.

### Praseodymium centrifuge recipe

- The final Indium output decreases from 12 dust to 8.
- The same reduction applies in the Industrial Centrifuge.

### GT++ Sphalerite vacuum furnace

- Three separate 64-dust Indium output stacks each become 32.

### GT++ Chalcopyrite vacuum furnace

- Indium output decreases from 30 dust to 20.

## Hungry Axe (Extra Utilities Healing Axe)

Extra Utilities' Healing Axe gives its holder one hunger point every 40 ticks while it is the selected hotbar
item. With Extra Utilities installed, the same axe takes one hunger point instead, never below zero. Saturation,
exhaustion and the food timer are preserved.

Nothing announces the change. The axe is an ordinary "Healing Axe" in NEI, chests and the creative tab. The first
time a stack is in a player's inventory (the inventory ticks every stack it holds, so picking it up, crafting it,
taking it from a chest and an admin giving it all count) the server marks the stack with the NBT flag
`gtnhextranerf_hungry`. Marked stacks are named "Hungry Axe" through `getItemStackDisplayName`, keep that name when
dropped or stored, and hunger drain applies to every axe whether it is marked yet or not. The mark is a plain NBT
tag, so it also shows as one tag in advanced tooltips.

`mixins.late.MixinHealingAxeHunger` injects at the start of `ItemHealingAxe.onUpdate` to set the mark, wraps the
`FoodStats.addStats` call to take hunger and overrides the display name. Hodgepodge's mixins on the same class touch
`onLeftClickEntity` and the constructor only. `items.HungryAxe` uses the shared `FoodStats` NBT API rather than the
client-only hunger setter. A Horizon-QA scenario exercises server-side player inventory ticks with the real axe.

## Features not present in the current version

For clarity, the active source does not contain:

- a shears crafting nerf;
- removal of the TConstruct no-gas Aluminium route;
- a small-batch circuit-9 Plutonium decay recipe;
- configuration switches for individual changes.
