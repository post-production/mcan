package ga.avven.mcan.annotations

import kotlin.reflect.KClass

import ga.avven.mcan.annotations.RecipeMapping

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class BlastingRecipe(val xp: Int = 0, val ticks: Int = 100, val mapping: RecipeMapping)
