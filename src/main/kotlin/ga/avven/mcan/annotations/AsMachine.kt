package ga.avven.mcan.annotations

import ga.avven.mcan.machine.MachineBlock
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AsMachine(val block: KClass<out MachineBlock>)
