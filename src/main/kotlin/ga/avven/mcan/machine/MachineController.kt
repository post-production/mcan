package ga.avven.mcan.machine

import ga.avven.mcan.Machine
import io.github.cottonmc.cotton.gui.CottonCraftingController
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import net.minecraft.container.BlockContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.recipe.RecipeType

typealias ControllerProvider = (syncId: Int, playerInv: PlayerInventory, ctx: BlockContext) -> MachineController
open class MachineController private constructor(machine: Machine, syncId: Int, playerInv: PlayerInventory, ctx: BlockContext): CottonCraftingController(RecipeType.SMELTING, syncId, playerInv, getBlockInventory(ctx), getBlockPropertyDelegate(ctx)) {
	companion object {
		fun create(machine: Machine): ControllerProvider {
			return {syncId: Int, playerInv: PlayerInventory, ctx: BlockContext ->
				object: MachineController(machine, syncId, playerInv, ctx) {}
			}
		}
	}

	init {
		// Allow the machine to specify its GUI
		val root = machine.setupGui(blockInventory, createPlayerInventoryPanel())

		setRootPanel(root)
		root.validate(this)
	}
}