package model.rider

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.PredefinedType.bool
import com.jetbrains.rd.generator.nova.PredefinedType.string
import com.jetbrains.rd.generator.nova.property
import com.jetbrains.rd.generator.nova.signal
import com.jetbrains.rider.model.nova.ide.SolutionModel

object InspectionCopyModel : Ext(SolutionModel.Solution) {
    init {
        signal("start", string)
        property("running", bool)
        property("result", string)
        property("error", string)
    }
}
