package net.horizonsend.ion.server.features.custom.items.mods.tool

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/**
 * A mod that modifies drops
 **/
interface DropModifier {
	val shouldDropXP: Boolean
	val usedTool: ItemStack?

	fun getDrop(block: Block): Collection<ItemStack>

	companion object {
		private val PICKAXE = ItemStack(Material.DIAMOND_PICKAXE, 1)

		val DEFAULT_DROP_PROVIDER = object : DropModifier {
			override val shouldDropXP: Boolean = true
			override val usedTool: ItemStack = PICKAXE

			override fun getDrop(block: Block): Collection<ItemStack> {
				return block.getDrops(PICKAXE)
			}
		}
	}
}