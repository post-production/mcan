package ga.avven.mcan.annotations

import kotlin.reflect.KClass

annotation class RecipeMapping(
	val id: String = "",
	val fromRegistry: KClass<*> = Nothing::class,
	val fromTag: String = "",
	val fromID: String = ""
)