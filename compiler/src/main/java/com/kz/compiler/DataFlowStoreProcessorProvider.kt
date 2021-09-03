package com.kz.compiler


import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSValueParameter
import com.kz.annotation.FlowStore
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

@AutoService(SymbolProcessorProvider::class)
class DataFlowStoreProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = DataFlowStoreProcessor(
        environment.codeGenerator
    )
}

class DataFlowStoreProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> = emptyList<KSAnnotated>().apply {
        resolver.getSymbolsWithAnnotation(FlowStore::class.qualifiedName.toString())
            .filterIsInstance<KSClassDeclaration>()
            .map { collectIr(it) }
            .forEach { codeGen(codeGenerator, it) }
    }
}

fun collectIr(declaration: KSClassDeclaration): Ir {
    val sourceFile = declaration.containingFile!!
    val pgName = declaration.packageName.asString()
    val className = declaration.simpleName.asString()
    val paramList = declaration.primaryConstructor!!.parameters.asSequence()
        .map { parameter: KSValueParameter ->
            val typeDeclaration = parameter.type.resolve().declaration
            val classDeclaration = typeDeclaration as KSClassDeclaration
            ParamIr(
                parameter.name!!.asString(),
                TypeIr(
                    classDeclaration.packageName.asString(),
                    classDeclaration.simpleName.asString()
                )
            )
        }
        .toList()

    return Ir(sourceFile, pgName, className, paramList)
}

fun codeGen(codeGenerator: CodeGenerator, ir: Ir) {
    val stateType = ClassName(ir.pgName, ir.className)

    val fileName = "${ir.className}Store"

    val initParameterSpec = ParameterSpec(Name.state, stateType)

    val primaryConstructor = FunSpec.constructorBuilder()
        .addModifiers(KModifier.PRIVATE)
        .addParameter(initParameterSpec)
        .build()

    val companionObjectSpec = TypeSpec.companionObjectBuilder().addFunction(
        FunSpec.builder("asStore")
            .receiver(stateType)
            .addStatement("return %L(%N = this)", fileName, initParameterSpec)
            .build()
    ).build()

    val backPropertySpec = PropertySpec.builder(
        Name.backFlow,
        Type.mutableStateFlow.parameterizedBy(stateType),
        KModifier.PRIVATE
    ).initializer(
        "%T(%N)",
        Type.mutableStateFlow,
        initParameterSpec
    ).build()

    val outPropertySpec = PropertySpec.builder(
        Name.outFlow,
        Type.stateFlow.parameterizedBy(stateType)
    ).getter(
        FunSpec.getterBuilder().addStatement("return %N", backPropertySpec).build()
    ).build()

    val propertyFunSpecs = ir.paramList.map {
        FunSpec.builder(it.name)
            .addModifiers(KModifier.SUSPEND)
            .addParameter(
                Name.block, ClassName(it.typeIr.pgName, it.typeIr.simpleName).run {
                    LambdaTypeName.get(receiver = this, returnType = this)
                }
            )
            .addStatement("val %L = %N.value", Name.oldState, backPropertySpec)
            .addStatement("val %L = %L.%L", Name.oldValue, Name.oldState, it.name)
            .addStatement(
                "val %L = %L.%L()",
                Name.newValue,
                Name.oldValue,
                Name.block
            )
            .addStatement(
                "val %L = %L.copy(%L = %L)",
                Name.newState,
                Name.oldState,
                it.name,
                Name.newValue
            )
            .addStatement("%N.emit(%L)", backPropertySpec, Name.newState)
            .build()
    }.toList()

    val typeSpec = TypeSpec.classBuilder(fileName)
        .primaryConstructor(primaryConstructor)
        .addType(companionObjectSpec)
        .addProperty(backPropertySpec)
        .addProperty(outPropertySpec)
        .addFunctions(propertyFunSpecs)
        .build()

    val fileSpec = FileSpec.builder(ir.pgName, fileName)
        .addType(typeSpec)
        .build()

    codeGenerator.createNewFile(
        Dependencies(true, ir.sourceFile),
        ir.pgName,
        fileName
    ).use { stream ->
        stream.writer().use { writer ->
            fileSpec.writeTo(writer)
        }
        stream.flush()
    }
}

object Type {
    private const val pgName = "kotlinx.coroutines.flow"
    private const val stateFlowName = "StateFlow"
    private const val mutableStateFlowName = "MutableStateFlow"

    val stateFlow = ClassName(pgName, stateFlowName)
    val mutableStateFlow = ClassName(pgName, mutableStateFlowName)
}

object Name {
    const val state = "state"
    const val backFlow = "_flow"
    const val outFlow = "flow"
    const val block = "block"
    const val oldState = "oldState"
    const val oldValue = "oldValue"
    const val newValue = "newValue"
    const val newState = "newState"
}

data class TypeIr(
    val pgName: String,
    val simpleName: String
)

data class ParamIr(
    val name: String,
    val typeIr: TypeIr
)

data class Ir(
    val sourceFile: KSFile,
    val pgName: String,
    val className: String,
    val paramList: List<ParamIr>
)
