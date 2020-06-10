package ga.avven.mcan.codegen

import com.beust.klaxon.json

import com.squareup.kotlinpoet.ClassName
import ga.avven.mcan.annotations.*

import kotlin.reflect.KClass
import java.io.File
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.MirroredTypeException

import net.minecraft.util.Identifier

class JSONGenerator {
	companion object {
		private fun mapKeys(errorHandle: String, identifiers: Map<String, Pair<String, Identifier>>, registry: String, id: String, tag: String): Pair<String, String> {
			var keyCount = 0
			var mapping: Pair<String, String> = Pair("", "")

			val fromReg = identifiers[registry]
			if (fromReg != null) {
				mapping = "item" to fromReg.second.toString()
				keyCount += 1
			}
			if (id.isNotEmpty()) {
				mapping = "item" to id
				keyCount += 1
			}
			if (tag.isNotEmpty()) {
				mapping = "tag" to tag
				keyCount += 1
			}

			// Make sure that there was a key supplied
			if (keyCount == 0)
				throw Exception("$errorHandle: No value set for key (fromRegistry = $registry, fromID = $id, fromTag = $tag)!")

			// Make sure ethat only 1 key was supplied
			if (keyCount != 1)
				throw Exception("$errorHandle: More than one value set for (fromRegistry = $registry, fromID = $id, fromTag = $tag)! $registry")

			// Return the final mapping
			return mapping
		}

		fun process(rootPath: String, set: MutableSet<out TypeElement>?, env: RoundEnvironment?, identifiers: Map<String, Pair<String, Identifier>>) {
			// Set up directories
			val root = File(rootPath)
			root.mkdir()

			val recipesRoot = File(root, "recipes")
			recipesRoot.mkdir()

			val lootRoot = File(root, "loot_tables")
			lootRoot.mkdir()

			val getEnclosingClass = { e: Element ->
				var toplevel = e
				while (toplevel.kind != ElementKind.CLASS || toplevel.simpleName.toString() == "Companion")
					toplevel = toplevel.enclosingElement

				toplevel
			}

			env?.getElementsAnnotatedWith(ShapedRecipe::class.java)?.forEach {
				val ann = it.getAnnotation(ShapedRecipe::class.java)
				val cls = getEnclosingClass(it)

				val canonicalName = cls.toString()
				val (_, id) = identifiers[canonicalName] ?: throw Exception("ShapedRecipe: Could not deduce ID from enclosing class $canonicalName")

				val pat = ((it as VariableElement).constantValue as String).let { raw ->
				 	raw.split("[").filter(String::isNotBlank).map { line ->
						line.split("]")[0]
					}
				}

				val keys = ann.mappings.map { key ->
					val reg = try {
						key.fromRegistry.toString()
					} catch (e: MirroredTypeException) {
						e.typeMirror.toString()
					}
					val mapping = mapKeys("ShapedRecipe", identifiers, reg, key.fromID, key.fromTag)

					// Return the final mapping
					if (key.id.isEmpty())
						throw Exception("ShapedRecipe: ID must be specified for a RecipeMapping!")

					key.id to mapping
				}
				val res = id.toString()
				val cnt = ann.count
				val name = "${id.path}_${it.simpleName}.generated.json"

				val asJson = json {
					obj(
						"type" to "minecraft:crafting_shaped",
						"key" to obj(keys.map { k ->
							k.first to obj(k.second)
						}),
						"pattern" to array(pat),
						"result" to obj(
							"item" to res,
							"count" to cnt
						)
					)
				}

				val recipe = File(recipesRoot, name)
				recipe.writeText(asJson.toJsonString())
			}

			env?.getElementsAnnotatedWith(ShapelessRecipe::class.java)?.forEach {
				val ann = it.getAnnotation(ShapelessRecipe::class.java)
				val cls = it.simpleName.toString()

				val canonicalName = it.toString()
				val (_, id) = identifiers[canonicalName] ?: throw Exception("ShapelessRecipe: Could not deduce ID from class $canonicalName.")

				val keys = ann.mappings.map { key ->
					val reg = try {
						key.fromRegistry.toString()
					} catch (e: MirroredTypeException) {
						e.typeMirror.toString()
					}

					mapKeys("ShapelessRecipe", identifiers, reg, key.fromID, key.fromTag)
				}
				val res = id.toString()
				val cnt = ann.count
				val name = "${id.path}_shapeless.generated.json"

				val asJson = json {
					obj(
						"type" to "minecraft:crafting_shapeless",
						"ingredients" to array(keys.map { k ->
							obj(k)
						}),
						"result" to obj(
							"item" to res,
							"count" to cnt
						)
					)
				}

				val recipe = File(recipesRoot, name)
				recipe.writeText(asJson.toJsonString())
			}

			env?.getElementsAnnotatedWith(BlastingRecipe::class.java)?.forEach {
				val ann = it.getAnnotation(BlastingRecipe::class.java)
				val cls = it.simpleName.toString()

				val canonicalName = it.toString()
				val (_, id) = identifiers[canonicalName] ?: throw Exception("BlastingRecipe: Could not deduce ID from class $canonicalName.")

				val reg = try {
						ann.mapping.fromRegistry.toString()
					} catch (e: MirroredTypeException) {
						e.typeMirror.toString()
					}
				val key = mapKeys("BlastingRecipe", identifiers, reg, ann.mapping.fromID, ann.mapping.fromTag)
				val res = id.toString()
				val name = "${id.path}_blasting.generated.json"

				val asJson = json {
					obj(
						"type" to "minecraft:blasting",
						"ingredient" to obj(key),
						"result" to res,
						"experience" to ann.xp,
						"cookingTime" to ann.ticks
					)
				}

				val recipe = File(recipesRoot, name)
				recipe.writeText(asJson.toJsonString())
			}

			env?.getElementsAnnotatedWith(SmeltingRecipe::class.java)?.forEach {
				val ann = it.getAnnotation(SmeltingRecipe::class.java)
				val cls = it.simpleName.toString()

				val canonicalName = it.toString()
				val (_, id) = identifiers[canonicalName] ?: throw Exception("SmeltingRecipe: Could not deduce ID from class $canonicalName.")

				val reg = try {
						ann.mapping.fromRegistry.toString()
					} catch (e: MirroredTypeException) {
						e.typeMirror.toString()
					}
				val key = mapKeys("SmeltingRecipe", identifiers, reg, ann.mapping.fromID, ann.mapping.fromTag)
				val res = id.toString()
				val name = "${id.path}_smelting.generated.json"

				val asJson = json {
					obj(
						"type" to "minecraft:smelting",
						"ingredient" to obj(key),
						"result" to res,
						"experience" to ann.xp,
						"cookingTime" to ann.ticks
					)
				}

				val recipe = File(recipesRoot, name)
				recipe.writeText(asJson.toJsonString())
			}

			env?.getElementsAnnotatedWith(DropsSelf::class.java)?.forEach {
				val cls = it.simpleName.toString()

				val canonicalName = it.toString()
				val (_, id) = identifiers[canonicalName] ?: throw Exception("DropsSelf: Could not deduce ID from class $canonicalName.")
				val name = "${id.path}.json"

				val asJson = json {
					obj(
						"type" to "minecraft:block",
						"pools" to array(
							obj(
								"rolls" to 1,
								"entries" to array(
									obj(
										"type" to "minecraft:item",
										"name" to id.toString()
									)
								),
								"conditions" to array(
									obj(
										"condition" to "minecraft:survives_explosion"
									)
								)
							)
						)
					)
				}

				val loot = File(lootRoot, name)
				loot.writeText(asJson.toJsonString())
			}

			env?.getElementsAnnotatedWith(DropsSome::class.java)?.forEach {
				val ann = it.getAnnotation(DropsSome::class.java)
				val cls = it.simpleName.toString()

				val canonicalName = it.toString()
				val (_, id) = identifiers[canonicalName] ?: throw Exception("DropsSelf: Could not deduce ID from class $canonicalName.")
				val name = "${id.path}.json"

				val reg = try {
						ann.item.fromRegistry.toString()
					} catch (e: MirroredTypeException) {
						e.typeMirror.toString()
					}
				val key = mapKeys("DropsSome", identifiers, reg, ann.item.fromID, "")

				val asJson = json {
					obj(
						"type" to "minecraft:block",
						"pools" to array(
							obj(
								"rolls" to 1,
								"entries" to array(
									obj(
										"type" to "minecraft:item",
										"name" to key.second
									)
								),
								"conditions" to array(
									obj(
										"condition" to "minecraft:survives_explosion"
									)
								),
								"functions" to array(
									obj(
										"function" to "set_count",
										"count" to obj(
											"min" to ann.min,
											"max" to ann.max
										)
									)
								)
							)
						)
					)
				}

				val loot = File(lootRoot, name)
				loot.writeText(asJson.toJsonString())
			}
		}
	}
}