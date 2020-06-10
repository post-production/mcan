package ga.avven.mcan.machine

import ga.avven.mcan.Machine
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.LootableContainerBlockEntity
import net.minecraft.container.BlockContext
import net.minecraft.container.Container
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.DefaultedList
import net.minecraft.util.Tickable

typealias EntitySupplier = () -> MachineEntity
open class MachineEntity private constructor(internal val machine: Machine, internal val block: MachineBlock, internal val container: ControllerProvider): LootableContainerBlockEntity(machine.entityType), Tickable {
	companion object {
		fun create(machine: Machine, block: MachineBlock, container: ControllerProvider): EntitySupplier {
			return {
				object: MachineEntity(machine, block, container) {}
			}
		}
	}

	private var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(machine.invSize, ItemStack.EMPTY)

	override fun createContainer(syncId: Int, playerInv: PlayerInventory): Container = container(syncId, playerInv, BlockContext.create(world, pos))
	override fun getInvStackList(): DefaultedList<ItemStack> = inventory
	override fun setInvStackList(list: DefaultedList<ItemStack>?) {
		this.inventory = list ?: DefaultedList.ofSize(invSize, ItemStack.EMPTY)
	}

	override fun getInvSize(): Int = machine.invSize
	override fun getContainerName(): Text = TranslatableText("container.${block.ID}")

	// Serializing
	override fun fromTag(tag: CompoundTag?): Unit {
		super.fromTag(tag)
		inventory = DefaultedList.ofSize(invSize, ItemStack.EMPTY)
		if (!deserializeLootTable(tag)) {
			Inventories.fromTag(tag, inventory)
		}
	}

	override fun toTag(tag: CompoundTag?): CompoundTag? {
		super.toTag(tag)
		if (!serializeLootTable(tag)) {
			Inventories.toTag(tag, inventory)
		}
		return tag
	}

	override fun tick() {
		machine.tick(this.world, this.pos, this)
	}
}