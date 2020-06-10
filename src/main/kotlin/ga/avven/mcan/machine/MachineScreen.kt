package ga.avven.mcan.machine

import ga.avven.mcan.Machine
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import net.minecraft.client.gui.screen.ingame.ContainerScreen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text


typealias ScreenSupplier = (container: MachineController, player: PlayerEntity?) -> MachineScreen
open class MachineScreen(private val machine: Machine, container: MachineController, player: PlayerEntity?): CottonInventoryScreen<MachineController>(container, player) {
	companion object {
		fun create(machine: Machine): ScreenSupplier {
			return {container: MachineController, player: PlayerEntity? ->
				object: MachineScreen(machine, container, player) {}
			}
		}
	}
}