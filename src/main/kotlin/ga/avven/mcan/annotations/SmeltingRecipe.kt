package ga.avven.mcan.annotations

import kotlin.reflect.KClass

import ga.avven.mcan.annotations.RecipeMapping

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SmeltingRecipe(val xp: Int = 0, val ticks: Int = 200, val mapping: RecipeMapping)
