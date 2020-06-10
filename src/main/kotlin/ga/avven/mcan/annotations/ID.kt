package ga.avven.mcan.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ID(val path: String, val namespace: String = "")
