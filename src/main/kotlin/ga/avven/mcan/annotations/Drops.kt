package ga.avven.mcan.annotations

import ga.avven.mcan.annotations.RecipeMapping

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DropsSome(val item: RecipeMapping, val min: Int, val max: Int)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DropsSelf()
