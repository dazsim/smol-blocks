# Smol Blocks

Cut basic blocks into slabs, vertical slabs, quarters and eighths with a saw, then place them. One block space holds up to 8 eighths, each from any source block.

## Saws

- Iron Saw: stick, iron ingot, iron ingot in a row.
- Diamond Saw: stick, diamond, diamond in a row. Lasts longer.

The saw loses one durability per cut and stays in the grid. Hard blocks in `smolblocks:needs_diamond_saw` (by default everything in `minecraft:needs_diamond_tool`, such as obsidian and ancient debris) need the diamond saw, and so do pieces cut from them.

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

Pieces go into the half or quarter of the space nearest where you click. Vertical slabs sit flat against the face you click (or face you when placed on a top or bottom face). Quarters lie across your view. Pieces can be added to a space that already has pieces as long as the cells are free. Mining a space breaks all of it and drops every piece.

## Which blocks can be cut

Full cube, solid, no block entity. Tags `smolblocks:cuttable` (always allowed) and `smolblocks:uncuttable` (never allowed) override this.
