package ga.avven.mcan.annotations

import kotlin.reflect.KClass

import ga.avven.mcan.annotations.RecipeMapping

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ShapelessRecipe(val count: Int = 1, vararg val mappings: RecipeMapping)
