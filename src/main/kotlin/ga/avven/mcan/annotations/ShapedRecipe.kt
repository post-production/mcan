package ga.avven.mcan.annotations

import ga.avven.mcan.annotations.RecipeMapping

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class ShapedRecipe(val count: Int = 1, vararg val mappings: RecipeMapping)
