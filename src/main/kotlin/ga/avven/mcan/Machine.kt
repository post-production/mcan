package ga.avven.mcan

import ga.avven.mcan.machine.MachineEntity
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WPanel
import io.github.cottonmc.cotton.gui.widget.WPlayerInvPanel
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.inventory.Inventory
import net.minecraft.util.Tickable
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

abstract class Machine {
	lateinit var entityType: BlockEntityType<out MachineEntity>

	val invSize: Int
		get() = getInventorySize()
	val width: Int
		get() = getBounds().first
	val height : Int
		get() = getBounds().second

	abstract fun getInventorySize(): Int
	abstract fun setupGui(blockInv: Inventory, playerInv: WPlayerInvPanel): WPanel
	abstract fun getBounds(): Pair<Int, Int>

	abstract fun tick(world: World?, pos: BlockPos, inv: MachineEntity)

	fun setType(v: BlockEntityType<out MachineEntity>) {
		this.entityType = v
	}
}