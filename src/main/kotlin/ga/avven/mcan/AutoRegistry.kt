package ga.avven.mcan

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class AutoRegistry() {
	companion object {
		var INSTANCE: AutoRegistry? = null
		fun get(): AutoRegistry = INSTANCE ?: throw Exception("AutoRegistry not initialized!")
	}

	interface Getter {
		abstract operator fun <T, R> getValue(thisRef: T, property: KProperty<*>): R
	}

	abstract fun ID(owner: KClass<*>): Getter
	abstract val BLOCK: Getter
	abstract fun BLOCK_ENTITY(owner: KClass<*>): Getter
	abstract fun ITEM(owner: KClass<*>): Getter

	init {
		INSTANCE = this
	}
}

val autoRegistry: AutoRegistry
	get() = AutoRegistry.get()
