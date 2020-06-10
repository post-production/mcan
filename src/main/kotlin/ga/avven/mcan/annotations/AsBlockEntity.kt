package ga.avven.mcan.annotations

import net.minecraft.block.Block
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AsBlockEntity(val block: KClass<out Block>)
