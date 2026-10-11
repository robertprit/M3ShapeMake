package com.compose.canvas.editor

import androidx.compose.ui.graphics.Color
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.PresetGeometry
import com.m3shapes.editor.ExpressiveShapeId
import com.m3shapes.editor.fitToViewport
import com.m3shapes.editor.toRoundedPolygon

private const val MATERIAL_SHAPE_SIZE = 256f

fun materialShapePath(shape: ExpressiveShapeId): PathGeometry {
    val polygon = shape.toRoundedPolygon().fitToViewport()
    val first = polygon.cubics.firstOrNull()
        ?: return PathGeometry(emptyList(), closed = true)
    return PathGeometry(
        segments = buildList {
            add(PathSegment.MoveTo(first.anchor0X, first.anchor0Y))
            polygon.cubics.forEach { cubic ->
                add(
                    PathSegment.CubicBezierTo(
                        cubic.control0X,
                        cubic.control0Y,
                        cubic.control1X,
                        cubic.control1Y,
                        cubic.anchor1X,
                        cubic.anchor1Y,
                    )
                )
            }
            add(PathSegment.Close)
        },
        closed = true,
    )
}

fun EditorState.insertMaterialShape(shape: ExpressiveShapeId) {
    val canvas = document.canvas
    val offsetStep = (document.allObjects().size % 6) * 14f
    addObjectToActiveLayer(
        VectorObject(
            name = shape.label,
            geometry = PresetGeometry(
                presetId = "material3:${shape.name}",
                width = MATERIAL_SHAPE_SIZE,
                height = MATERIAL_SHAPE_SIZE,
                definition = materialShapePath(shape),
            ),
            transform = ObjectTransform(
                x = (canvas.width - MATERIAL_SHAPE_SIZE) / 2f + offsetStep,
                y = (canvas.height - MATERIAL_SHAPE_SIZE) / 2f + offsetStep,
            ),
            fill = FillStyle(color = Color(0xFF6750A4)),
        )
    )
    setTool(EditorTool.Select)
}
