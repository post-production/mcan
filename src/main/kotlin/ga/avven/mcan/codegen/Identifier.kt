package ga.avven.mcan.codegen

data class Identifier(val namespace: String, val path: String) {
	override fun toString(): String = "$namespace:$path"
}