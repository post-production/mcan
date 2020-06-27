package ga.avven.mcan.codegen

import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

import com.google.auto.service.AutoService
import com.google.common.base.CaseFormat

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

import ga.avven.mcan.annotations.*
import ga.avven.mcan.AutoRegistry
import javax.lang.model.type.MirroredTypeException

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
@SupportedOptions(Generator.GENERATED_KAPT)
class Generator : AbstractProcessor() {
	companion object {
		const val GENERATED_KAPT = "kapt.kotlin.generated"
		const val PACKAGE = "ga.avven.mcan"
		const val CLASS_NAME = "AutoRegistry_impl"

		val MINECRAFT_ID: ClassName = ClassName("net.minecraft.util", "Identifier")
		val MINECRAFT_BLOCK: ClassName = ClassName("net.minecraft.block", "Block")
		val MINECRAFT_BLOCK_ITEM: ClassName = ClassName("net.minecraft.item", "BlockItem")
		val MINECRAFT_BLOCK_ENTITY_TYPE: ClassName = ClassName("net.minecraft.block.entity", "BlockEntityType")
		val MINECRAFT_ITEM: ClassName = ClassName("net.minecraft.item", "Item")
	}

	internal val generatedSourcesRoot by lazy { processingEnv.options[GENERATED_KAPT].orEmpty() }
	internal var namespace = "modid"
	internal val identifierCache: MutableSet<String> = mutableSetOf()
	internal val identifiers: MutableMap<String, Pair<String, Identifier>> = mutableMapOf()
	internal val blocks: MutableMap<String, Pair<String, String>> = mutableMapOf()

	internal val classBuilder = TypeSpec.classBuilder(CLASS_NAME)
		.addModifiers(KModifier.PUBLIC)
		.addSuperinterface(ClassName("net.fabricmc.api", "ModInitializer"))
		.superclass(ClassName(PACKAGE, "AutoRegistry"))
	internal val funBuilder = FunSpec.builder("onInitialize")
		.addModifiers(KModifier.OVERRIDE)

	override fun getSupportedAnnotationTypes(): MutableSet<String> {
		return mutableSetOf(
			AsBlock::class.java.name,
			AsBlockEntity::class.java.name,
			AsBlockItem::class.java.name,
			AsItem::class.java.name,
			BlastingRecipe::class.java.name,
			DropsSome::class.java.name,
			DropsSelf::class.java.name,
			ID::class.java.name,
			ModID::class.java.name,
			ShapedRecipe::class.java.name,
			ShapelessRecipe::class.java.name,
			SmeltingRecipe::class.java.name
		)
	}

	override fun getSupportedSourceVersion(): SourceVersion {
		return SourceVersion.latest()
	}

	override fun process(set: MutableSet<out TypeElement>?, env: RoundEnvironment?): Boolean {
		val T = TypeVariableName("T")
		val R = TypeVariableName("R")
		val createGetter = { whenCond: String ->
			FunSpec.builder("getValue")
				.addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
				.addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
				.addTypeVariables(listOf(T, R))
				.addParameter("thisRef", T)
				.addParameter("property", KProperty::class.asClassName().parameterizedBy(STAR))
				.returns(R)
				.beginControlFlow("return when ($whenCond)")
		}

		val idGetter    = createGetter("owner")
		val blockGetter = createGetter("property.returnType")
		val itemGetter  = createGetter("owner")
		val blockEntityGetter = createGetter("owner")

		val agen = FileSpec.builder(PACKAGE, CLASS_NAME)
		var done = false

		env?.getElementsAnnotatedWith(ModID::class.java)?.let {
			if (it.size == 0)
				return@let

			if (it.size > 1)
				throw Exception("@ModID only allowed once")

			val ann = it.first().getAnnotation(ModID::class.java)
			if (ann.namespace.isEmpty())
				throw Exception("ModID cannot be empty!")

			this.namespace = ann.namespace
		}

		env?.getElementsAnnotatedWith(AsBlock::class.java)?.forEach {
			generateBlock(it, idGetter, blockGetter)

			done = true
		}
		env?.getElementsAnnotatedWith(AsBlockItem::class.java)?.forEach {
			generateBlockItem(it, idGetter, blockGetter, itemGetter)

			done = true
		}
		env?.getElementsAnnotatedWith(AsBlockEntity::class.java)?.forEach {
			generateBlockEntity(it, idGetter, blockEntityGetter)

			done = true
		}
		env?.getElementsAnnotatedWith(AsItem::class.java)?.forEach {
			generateItem(it, idGetter, itemGetter)

			done = true
		}

		// JSON Files
		JSONGenerator.process("$generatedSourcesRoot/json", set, env, this.identifiers)

		// Each annotation is processed exactly twice. We check here to see if we have
		//   already processed these tags, if so we do nothing.
		if (!done)
			return false

		val file = File(generatedSourcesRoot)
		file.mkdir()

		// Utility function for finishing the getters
		val finishGetter = { getter: FunSpec.Builder ->
			getter
				.addStatement("else -> throw Exception(%S + property.name)", "Cannot find type: ")
				.endControlFlow()
		}
		finishGetter(idGetter)
		finishGetter(blockGetter)
		finishGetter(itemGetter)
		finishGetter(blockEntityGetter)

		// ID and Item Getters need to be passed an owner class, so we define an enclosing function
		//   that returns the getter while also lambda capturing the owner
		val getEnclosingGetter = { name: String, getter: FunSpec.Builder ->
			FunSpec.builder(name)
				.addModifiers(KModifier.OVERRIDE)
				.addParameter("owner", KClass::class.asClassName().parameterizedBy(STAR))
				.returns(AutoRegistry.Getter::class)
				.addCode(
					CodeBlock.of(
						"return %L",
						TypeSpec.anonymousClassBuilder()
							.addSuperinterface(AutoRegistry.Getter::class)
							.addFunction(getter.build())
							.build()
					)
				)
		}

		// Utility function to add a getter
		val addGetter = { propName: String, getterFun: FunSpec.Builder ->
			PropertySpec.builder(
				propName,
				AutoRegistry.Getter::class
			).addModifiers(KModifier.OVERRIDE).initializer(
				 // Only way to convert anonymous object to CodeBlock is to treat them
				 //   like literals :(
				CodeBlock.of(
					"%L",
					TypeSpec.anonymousClassBuilder()
						.addSuperinterface(AutoRegistry.Getter::class)
						.addFunction(getterFun.build())
						.build()
				)
			).build()
		}

		agen
			.addImport("kotlin.reflect", "KProperty")
			.addImport("kotlin.reflect.full", "createType")
			.addImport("java.util.function", "Supplier")
			.addImport("net.minecraft.util.registry", "Registry")
			.addImport("net.minecraft.util", "Identifier")
			.addImport("net.minecraft.item", "BlockItem", "Item", "ItemGroup")
			.addImport("net.minecraft.block.entity", "BlockEntityType")
			.addImport("net.fabricmc.api", "ModInitializer")
			.addType(classBuilder
				.addFunction(funBuilder.build())
				.addFunction(getEnclosingGetter("ID", idGetter).build())
				.addProperty(addGetter("BLOCK", blockGetter))
				.addFunction(getEnclosingGetter("BLOCK_ENTITY", blockEntityGetter).build())
				.addFunction(getEnclosingGetter("ITEM", itemGetter).build())
				.build()
			)
			.build()
			.writeTo(file)

		return true
	}

	internal fun getPackage(e: Element): String {
		var toplevel = e.enclosingElement
		var hierarchy = ""
		while (toplevel.enclosingElement != null) {
			hierarchy = toplevel.simpleName.toString() + "." + hierarchy
			toplevel = toplevel.enclosingElement
		}

		return processingEnv.elementUtils.getPackageOf(toplevel).toString() + "." + hierarchy
	}

	internal fun getIdentifier(e: Element): Identifier {
		val id: ID? = e.getAnnotation(ID::class.java)

		val toPath = { path: String ->
			CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, path)
		}

		return id?.let {
			val ns = if (it.namespace.isNotEmpty()) it.namespace else this.namespace
			val path = if(it.path.isNotEmpty()) it.path else toPath(e.simpleName.toString())

			Identifier(ns, path)
		} ?: Identifier(this.namespace, toPath(e.simpleName.toString()))
	}

	private fun generateBlock(e: Element, idGen: FunSpec.Builder, valueGen: FunSpec.Builder) {
		val className     = e.simpleName.toString()
		val canonicalName = ClassName(getPackage(e), className)

		val ann          = e.getAnnotation(AsBlock::class.java)
		val idLiteral    = getIdentifier(e)
		val prefix       = idLiteral.namespace + "_" + idLiteral.path
		val idParam      = (prefix + "_id").toUpperCase()
		val idBlockParam = (prefix + "_block").toUpperCase()

		// Add block to map of blocks
		this.blocks[canonicalName.toString()] = Pair(idParam, idBlockParam)

		// Save the ID to the map
		this.identifiers[canonicalName.toString()] = Pair(idParam, idLiteral)

		// Check if ID already exists and reuse
		if (!this.identifierCache.contains(idParam)) {
			this.identifierCache.add(idParam)
			classBuilder.addProperty(
				PropertySpec.builder(idParam, MINECRAFT_ID).initializer("Identifier(%S)", idLiteral.toString()).build()
			)
		}

		// First add the Block
		classBuilder
			.addProperty(
				PropertySpec.builder(idBlockParam, MINECRAFT_BLOCK).initializer("%L()", canonicalName).build()
			)
		funBuilder
			.addStatement("Registry.register(Registry.BLOCK, %L, %L)", idParam, idBlockParam)
		idGen
			.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, idParam, "Could not fetch ID from AutoRegistry")
		valueGen
			.addStatement("%L::class.createType() -> %L as? R ?: throw Exception(%S)", canonicalName, idBlockParam, "Could not fetch Block from AutoRegistry")
	}

	private fun generateBlockEntity(e: Element, idGen: FunSpec.Builder, blockEntityGen: FunSpec.Builder) {
		val className     = e.simpleName.toString()
		val canonicalName = ClassName(getPackage(e), className)

		val ann           = e.getAnnotation(AsBlockEntity::class.java)
		val idLiteral     = getIdentifier(e)
		val prefix        = idLiteral.namespace + "_" + idLiteral.path
		val bEntityParam  = (prefix + "_block").toUpperCase()
		val blockName     = try {
			ann.block.qualifiedName
		} catch (e: MirroredTypeException) {
			e.typeMirror.toString()
		}

		val (idParam, block) = this.blocks[blockName] ?: throw Exception("BlockEntity owner not found!")

		// Check if ID already exists and reuse
		if (!this.identifierCache.contains(idParam)) {
			this.identifierCache.add(idParam)
			classBuilder.addProperty(
				PropertySpec.builder(idParam, MINECRAFT_ID).initializer("Identifier(%S)", idLiteral.toString()).build()
			)
		}

		// First add the Block
		classBuilder
			.addProperty(
				PropertySpec.builder(bEntityParam, MINECRAFT_BLOCK_ENTITY_TYPE.parameterizedBy(canonicalName)).mutable().addModifiers(KModifier.LATEINIT).build()
			)
		funBuilder
			.addCode("""
				|%L = Registry.register(
				|	Registry.BLOCK_ENTITY_TYPE,
				|	%L,
				|	BlockEntityType.Builder.create(
				|		Supplier<%L> { %L() },
				|		%L
				|	).build(null)
				|)
				|
			""".trimMargin(), bEntityParam, idParam, canonicalName, canonicalName, block).build()
		idGen
			.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, idParam, "Could not fetch ID from AutoRegistry")
		blockEntityGen
			.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, bEntityParam, "Could not fetch ID from AutoRegistry")
	}

	private fun generateBlockItem(e: Element, idGen: FunSpec.Builder, valueGen: FunSpec.Builder, itemGen: FunSpec.Builder) {
		val className     = e.simpleName.toString()
		val canonicalName = ClassName(getPackage(e), className)

		val ann          = e.getAnnotation(AsBlockItem::class.java)
		val idLiteral    = getIdentifier(e)
		val prefix       = idLiteral.namespace + "_" + idLiteral.path
		val idParam      = (prefix + "_id").toUpperCase()
		val idBlockParam = (prefix + "_block").toUpperCase()
		val idItemParam  = (prefix + "_item").toUpperCase()

		// Add block to map of blocks
		this.blocks[canonicalName.toString()] = Pair(idParam, idBlockParam)

		// Save the ID to the map
		this.identifiers[canonicalName.toString()] = Pair(idParam, idLiteral)

		// Check if ID already exists and reuse
		if (!this.identifierCache.contains(idParam)) {
			this.identifierCache.add(idParam)
			classBuilder.addProperty(
				PropertySpec.builder(idParam, MINECRAFT_ID).initializer("Identifier(%S)", idLiteral.toString()).build()
			)
		}

		classBuilder
			.addProperty(
				PropertySpec.builder(idBlockParam, MINECRAFT_BLOCK).initializer("%L()", canonicalName).build()
			)
			.addProperty(
				PropertySpec.builder(idItemParam, MINECRAFT_BLOCK_ITEM).initializer("BlockItem(%L, Item.Settings().group(ItemGroup.%L))", idBlockParam, ann.group).build()
			)
		funBuilder
			.addStatement("Registry.register(Registry.BLOCK, %L, %L)", idParam, idBlockParam)
			.addStatement("Registry.register(Registry.ITEM, %L, %L)", idParam, idItemParam)
		idGen
			.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, idParam, "Could not fetch ID from AutoRegistry")
		valueGen
			.addStatement("%L::class.createType() -> %L as? R ?: throw Exception(%S)", canonicalName, idBlockParam, "Could not fetch Block from AutoRegistry")
		itemGen
			.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, idItemParam, "Could not fetch Item from AutoRegistry")
	}

	private fun generateItem(e: Element, idGen: FunSpec.Builder, itemGen: FunSpec.Builder) {
		val className     = e.simpleName.toString()
		val canonicalName = ClassName(getPackage(e), className)

		val ann          = e.getAnnotation(AsBlockItem::class.java)
		val idLiteral    = getIdentifier(e)
		val prefix       = idLiteral.namespace + "_" + idLiteral.path
		val idParam      = (prefix + "_id").toUpperCase()
		val idItemParam  = (prefix + "_item").toUpperCase()

		// Save the ID to the map
		this.identifiers[canonicalName.toString()] = Pair(idParam, idLiteral)

		// Check if ID already exists and reuse
		if (!this.identifierCache.contains(idParam)) {
			this.identifierCache.add(idParam)
			classBuilder.addProperty(
				PropertySpec.builder(idParam, MINECRAFT_ID).initializer("Identifier(%S)", idLiteral.toString()).build()
			)
		}

		classBuilder
			.addProperty(
				PropertySpec.builder(idItemParam, MINECRAFT_ITEM).initializer("%L()", canonicalName).build()
			)
		funBuilder
			.addStatement("Registry.register(Registry.ITEM, %L, %L)", idParam, idItemParam)
		idGen
			.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, idParam, "Could not fetch ID from AutoRegistry")
		itemGen
			.addStatement("%L::class -> %L as? R ?: throw Exception(%S)", canonicalName, idItemParam, "Could not fetch Item from AutoRegistry")
	}
}
