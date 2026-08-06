@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package com.jetbrains.rd.ide.model

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [InspectionCopyModel.kt:10]
 */
class InspectionCopyModel private constructor(
    private val _start: RdSignal<String>,
    private val _running: RdOptionalProperty<Boolean>,
    private val _result: RdOptionalProperty<String>,
    private val _error: RdOptionalProperty<String>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
        }
        
        
        
        
        
        const val serializationHash = 4690292460081841852L
        
    }
    override val serializersOwner: ISerializersOwner get() = InspectionCopyModel
    override val serializationHash: Long get() = InspectionCopyModel.serializationHash
    
    //fields
    val start: ISignal<String> get() = _start
    val running: IOptProperty<Boolean> get() = _running
    val result: IOptProperty<String> get() = _result
    val error: IOptProperty<String> get() = _error
    //methods
    //initializer
    init {
        _running.optimizeNested = true
        _result.optimizeNested = true
        _error.optimizeNested = true
    }
    
    init {
        bindableChildren.add("start" to _start)
        bindableChildren.add("running" to _running)
        bindableChildren.add("result" to _result)
        bindableChildren.add("error" to _error)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdSignal<String>(FrameworkMarshallers.String),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<String>(FrameworkMarshallers.String),
        RdOptionalProperty<String>(FrameworkMarshallers.String)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("InspectionCopyModel (")
        printer.indent {
            print("start = "); _start.print(printer); println()
            print("running = "); _running.print(printer); println()
            print("result = "); _result.print(printer); println()
            print("error = "); _error.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): InspectionCopyModel   {
        return InspectionCopyModel(
            _start.deepClonePolymorphic(),
            _running.deepClonePolymorphic(),
            _result.deepClonePolymorphic(),
            _error.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val Solution.inspectionCopyModel get() = getOrCreateExtension("inspectionCopyModel", ::InspectionCopyModel)

