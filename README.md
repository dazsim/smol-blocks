# Smol Blocks

Cut basic blocks into slabs, vertical slabs, quarters and eighths with a saw, then place them. One block space holds up to 8 eighths, each from any source block.

## Versions

| Minecraft | Loaders | Java |
| --- | --- | --- |
| 1.20.1 | Fabric, Forge | 17 |
| 1.21.1 | Fabric, NeoForge | 21 |
| 26.1.2 | Fabric, NeoForge | 25 |
| 26.2 | Fabric, NeoForge | 25 |
| 26.3 | Fabric, NeoForge | 25 |

Fabric builds need Fabric API. Every version and loader has the same items, recipes, tags and rendering.

## Guide

When Simple Books is installed, craft a book with an iron nugget to get an in-game guide to cutting, placing, precision pickaxes, waterlogging and what can be cut.

## Shapes

| Shape | Cells | Size |
| --- | --- | --- |
| Slab | 4 | full width and depth, half height |
| Vertical Slab | 4 | full width and height, half depth |
| Quarter | 2 | full width, half height, half depth |
| Vertical Quarter | 2 | full height, half width, half depth |
| Eighth | 1 | half in every direction |

## Saws

- Iron Saw: stick, iron ingot, iron ingot in a row. 250 cuts.
- Diamond Saw: stick, diamond, diamond in a row. 1561 cuts.

The saw loses one durability per cut and stays in the grid. Hard blocks in `smolblocks:needs_diamond_saw` (by default everything in `minecraft:needs_diamond_tool`, such as obsidian and ancient debris) need the diamond saw, and so do pieces cut from them.

## Precision pickaxes

- Iron Precision Pickaxe: iron ingot, iron ingot, iron nugget across the top row, with 2 sticks down the middle column. 250 uses.
- Diamond Precision Pickaxe: diamond, diamond, iron nugget across the top row, with 2 sticks down the middle column. 1561 uses.

Mining a space with a precision pickaxe breaks only the piece you are looking at and drops it as an eighth. The drop follows the source block's own rules, so glass needs Silk Touch, stone needs a pickaxe, and a diamond-tier block needs the diamond precision pickaxe. Mining speed comes from that piece's source block. Each piece costs 1 durability. When the last piece goes, the space turns into air, or water if it was waterlogged. On any other block a precision pickaxe works like the vanilla pickaxe of its tier and takes the same enchantments. Any other tool still breaks the whole space at once.

## Cutting

Put a saw and one block or piece in the crafting grid. Each cut gives 2 pieces.

- Saw directly above or below: horizontal cut.
- Saw beside or diagonal: vertical cut.

| Input | Horizontal cut | Vertical cut |
| --- | --- | --- |
| Full block | 2 Slabs | 2 Vertical Slabs |
| Slab | 2 Quarters | 2 Quarters |
| Vertical Slab | 2 Quarters | 2 Vertical Quarters |
| Quarter or Vertical Quarter | 2 Eighths | 2 Eighths |

## Placing

Pieces go into the half or quarter of the space nearest where you click. Vertical slabs sit flat against the face you click (or face you when placed on a top or bottom face). Quarters lie across your view. Pieces can be added to a space that already has pieces as long as the cells are free. Clicking grass, snow or another replaceable block puts the piece in that space. Mining a space with any tool other than a precision pickaxe breaks all of it and drops every piece you could harvest with the tool in hand, so stone pieces mined by hand drop nothing. Mining speed and blast resistance come from the source blocks. Pick block gives the piece you are looking at on NeoForge and Forge, and the first piece in the space on Fabric.

Opaque pieces block light like the full block would on their faces, and pieces cut from glowing blocks give off light.

## Mixing

Every cell remembers its own source block, so one space can mix stone, glass, wood and anything else that can be cut. Each cell keeps its source's colour (grass, leaves and other tinted blocks follow the biome), light level and look. Faces between two cells are hidden when the cell beside them is opaque, and glass next to the same glass hides the shared face like full glass blocks do, including across neighbouring spaces. A grass block cut into lower pieces still shows its green side strip on the top of each side.

## Waterlogging

A space with any empty cell can hold water, like a vanilla slab. Placing a piece into water keeps the water, and buckets work as usual. A face only holds water back when all 4 cells touching it are filled, so a bottom slab keeps water above it and glass vertical slabs make a fish tank wall. A completely filled space holds no water.

Water only draws in the empty cells of the space, so it never shows through opaque pieces. Behind glass and other see-through pieces it shows the way it does behind a glass block, whether the glass is in the same space as the water or in the next one.

## Which blocks can be cut

Full cube, solid, no block entity. Block tags change this, and a datapack can add to them:

| Tag | Effect | Default |
| --- | --- | --- |
| `smolblocks:cuttable` | always allowed | glass and stained glass |
| `smolblocks:uncuttable` | never allowed | redstone parts, utility blocks, concrete powder, infested blocks, bedrock |
| `smolblocks:needs_diamond_saw` | needs the diamond saw | `#minecraft:needs_diamond_tool` |
| `smolblocks:top_strip` | lower pieces show the top strip of the side texture | grass block, mycelium, podzol, nylium |

Blocks whose side has a tinted overlay, like grass, get the strip without being in `top_strip`.

## JEI

On 26.1.2 NeoForge with JEI installed, example cuts show up under crafting, and the saws and precision pickaxes have info pages. The other builds have no recipe viewer support yet; the recipes still work in the crafting grid.

## Known limits

- Only the outer faces of a source block's model are used, so blocks with raised or inset detail look flat when cut.
- Blocks with random texture variants use the same variant in every cell.
- Pieces copy a block's look, light and drops, but not its particles or other effects, such as a steam vent's steam.
- Spaces with pieces cannot be pushed by pistons.
- Moving a world with pieces to a different Minecraft version is not supported.
- Fabric on 1.20.1 and 1.21.1 renders through the Fabric renderer API. With Sodium, install Indium as well.
- The crack overlay while mining with a precision pickaxe covers the whole space, not just the piece being mined.
- Shader packs and connected texture mods are untested.
- No EMI or REI support.
