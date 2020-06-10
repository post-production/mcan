package ga.avven.mcan.machine

import java.util.function.Supplier
import kotlin.reflect.KClass

import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.LootableContainerBlockEntity
import net.minecraft.container.Container
import net.minecraft.entity.LivingEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult

open class MachineBlock(settings: Block.Settings): BlockWithEntity(settings) {
	// We need a way to keep track of which BlockEntity corresponds to this block
	internal lateinit var entity: Supplier<MachineEntity>
	internal lateinit var entityType: KClass<out MachineEntity>
	lateinit var ID: Identifier

	fun register(id: Identifier, en: Supplier<MachineEntity>) {
		this.ID = id
		this.entity = en
		this.entityType = en.get()::class
	}

	// A side effect of extending BlockWithEntity is it changes the render type to INVISIBLE, so we have to revert this
	override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
	override fun createBlockEntity(view: BlockView): BlockEntity = entity.get()
	override fun onPlaced(
			world: World,
			pos: BlockPos,
			state: BlockState,
			placer: LivingEntity?,
			stack: ItemStack
	) {
		if (stack.hasCustomName()) {
			val blockEntity = world.getBlockEntity(pos)
			if (entityType.isInstance(blockEntity)) {
				(blockEntity as LootableContainerBlockEntity).customName = stack.name
			}
		}
	}

	override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
		if (!world.isClient) {
			val blockEntity = world.getBlockEntity(pos)
			if (entityType.isInstance(blockEntity)) {
				ContainerProviderRegistry.INSTANCE.openContainer(ID, player) { buf -> buf.writeBlockPos(pos) }
			}
		}
		return ActionResult.SUCCESS
	}

	override fun onBlockRemoved(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
		if (state.block != newState.block) {
			val blockEntity = world.getBlockEntity(pos)
			if (entityType.isInstance(blockEntity)) {
				ItemScatterer.spawn(world, pos, blockEntity as Inventory)
				// update comparators
				world.updateHorizontalAdjacent(pos, this)
			}

			// TODO: If this is deprecated, then what should we use?
			super.onBlockRemoved(state, world, pos, newState, moved)
		}
	}

	override fun hasComparatorOutput(state: BlockState) = true
	override fun getComparatorOutput(state: BlockState?, world: World?, pos: BlockPos?): Int {
		return Container.calculateComparatorOutput(world?.getBlockEntity(pos))
	}
}