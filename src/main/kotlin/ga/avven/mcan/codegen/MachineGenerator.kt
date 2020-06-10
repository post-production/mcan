package ga.avven.mcan.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec

import ga.avven.mcan.annotations.AsMachine
import net.minecraft.block.BlockWithEntity
import net.minecraft.item.BlockItem

import net.minecraft.util.Identifier

import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException

fun Generator.processMachine(rootPath: String, set: MutableSet<out TypeElement>?, env: RoundEnvironment?, fileGen: FileSpec.Builder, idGen: FunSpec.Builder, blockEntityGen: FunSpec.Builder, valueGen: FunSpec.Builder, itemGen: FunSpec.Builder) {
	env?.getElementsAnnotatedWith(AsMachine::class.java)?.forEach {
		val className     = it.simpleName.toString()
		val canonicalName = it.toString()

		val ann       = it.getAnnotation(AsMachine::class.java)
		val idLiteral = getIdentifier(it)
		val prefix    = idLiteral.namespace + "_" + idLiteral.path
		val machine   = (prefix + "_machine").toUpperCase()
		val container = (prefix + "_container").toUpperCase()
		val mblock    = (prefix + "_mblock").toUpperCase()
		val entity    = (prefix + "_entity").toUpperCase()
		val type      = (prefix + "_type").toUpperCase()
		val blockName = try {
			ann.block.qualifiedName
		} catch (e: MirroredTypeException) {
			e.typeMirror.toString()
		}

		val (idParam, block) = this.blocks[blockName] ?: throw Exception("BlockEntity owner not found!")

		// Add the needed imports
		fileGen
			.addImport("ga.avven.mcan.machine", "MachineContainer", "MachineEntity", "MachineBlock")

		funBuilder
			.addStatement("val %L = %L as MachineBlock", mblock, block)
			.addStatement("val %L = %L()", machine, canonicalName)
			.addStatement("val %L = MachineContainer.create(%L)", container, machine)
			.addStatement("val %L = MachineEntity.create(%L, %L, %L)", entity, machine, mblock, container)
			.addStatement("val %L = Registry.register(Registry.BLOCK_ENTITY_TYPE, %L, BlockEntityType.Builder.create(Supplier<MachineEntity> { %L() }, %L).build(null))", type, idParam, entity, block)
			.addStatement("%L.entityType = %L", machine, type)
			.addStatement("%L.register(%L, Supplier { %L() })", mblock, idParam, entity)

		// Add block to map of blocks
		// this.blocks[canonicalName.toString()] = Pair(idParam, idBlockParam)

		// // Save the ID to the map
		// this.identifiers[canonicalName.toString()] = Pair(idParam, idLiteral)

		// Check if ID already exists and reuse
		// if (!this.identifierCache.contains(idParam)) {
		// 	this.identifierCache.add(idParam)
		// 	classBuilder.addProperty(
		// 		PropertySpec.builder(idParam, Identifier::class).initializer("Identifier(%S)", idLiteral.toString()).build()
		// 	)
		// }

		// classBuilder
		// 	.addProperty(
		// 		PropertySpec.builder(idBlockParam, BlockWithEntity::class).initializer("%L()", canonicalName).build()
		// 	)
		// 	.addProperty(
		// 		PropertySpec.builder(idItemParam, BlockItem::class).initializer("BlockItem(%L, Item.Settings().group(ItemGroup.%L))", idBlockParam, ann.group).build()
		// 	)
		// funBuilder
		// 	.addStatement("Registry.register(Registry.BLOCK, %L, %L)", idParam, idBlockParam)
		// 	.addStatement("Registry.register(Registry.ITEM, %L, %L)", idParam, idItemParam)
		// idGen
		// 	.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, idParam, "Could not fetch ID from AutoRegistry")
		// valueGen
		// 	.addStatement("%L::class.createType() -> %L as? R ?: throw Exception(%S)", canonicalName, idBlockParam, "Could not fetch Block from AutoRegistry")
		// itemGen
		// 	.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, idItemParam, "Could not fetch Item from AutoRegistry")
	}
}