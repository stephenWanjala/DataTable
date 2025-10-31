package io.github.stephenwanjala.datatable


import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Utility delegate to construct a Material icon with default size information.
 * This is used by generated icons, and should not be used manually.
 *
 * @param name the full name of the generated icon
 * @param autoMirror determines if the vector asset should automatically be mirrored for right to
 * left locales
 * @param block builder lambda to add paths to this vector asset
 */
internal inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> ImageVector.Builder
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = MaterialIconDimension.dp,
    defaultHeight = MaterialIconDimension.dp,
    viewportWidth = MaterialIconDimension,
    viewportHeight = MaterialIconDimension,
    autoMirror = autoMirror
).block().build()

/**
 * Adds a vector path to this icon with Material defaults.
 *
 * @param fillAlpha fill alpha for this path
 * @param strokeAlpha stroke alpha for this path
 * @param pathFillType [PathFillType] for this path
 * @param pathBuilder builder lambda to add commands to this path
 */
internal inline fun ImageVector.Builder.materialPath(
    fillAlpha: Float = 1f,
    strokeAlpha: Float = 1f,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: PathBuilder.() -> Unit
) =
// TODO: b/146213225
// Some of these defaults are already set when parsing from XML, but do not currently exist
    // when added programmatically. We should unify these and simplify them where possible.
    path(
        fill = SolidColor(Color.Black),
        fillAlpha = fillAlpha,
        stroke = null,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Bevel,
        strokeLineMiter = 1f,
        pathFillType = pathFillType,
        pathBuilder = pathBuilder
    )

// All Material icons (currently) are 24dp by 24dp, with a viewport size of 24 by 24.
@PublishedApi
internal const val MaterialIconDimension = 24f

internal val ArrowDropDown: ImageVector
    get() {
        if (_arrowDropDown != null) {
            return _arrowDropDown!!
        }
        _arrowDropDown = materialIcon(name = "Filled.ArrowDropDown") {
            materialPath {
                moveTo(7.0f, 10.0f)
                lineToRelative(5.0f, 5.0f)
                lineToRelative(5.0f, -5.0f)
                close()
            }
        }
        return _arrowDropDown!!
    }

private var _arrowDropDown: ImageVector? = null


internal val KeyboardArrowUp: ImageVector
    get() {
        if (_keyboardArrowUp != null) {
            return _keyboardArrowUp!!
        }
        _keyboardArrowUp = materialIcon(name = "Filled.KeyboardArrowUp") {
            materialPath {
                moveTo(7.41f, 15.41f)
                lineTo(12.0f, 10.83f)
                lineToRelative(4.59f, 4.58f)
                lineTo(18.0f, 14.0f)
                lineToRelative(-6.0f, -6.0f)
                lineToRelative(-6.0f, 6.0f)
                close()
            }
        }
        return _keyboardArrowUp!!
    }

private var _keyboardArrowUp: ImageVector? = null

internal val KeyboardDoubleArrowLeft: ImageVector
    get() {
        if (_keyboardDoubleArrowLeft != null) {
            return _keyboardDoubleArrowLeft!!
        }
        _keyboardDoubleArrowLeft = materialIcon(name = "Filled.KeyboardDoubleArrowLeft") {
            materialPath {
                moveTo(17.59f, 18.0f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(-4.58f, -4.59f)
                lineToRelative(4.58f, -4.59f)
                lineToRelative(-1.41f, -1.41f)
                lineToRelative(-6.0f, 6.0f)
                close()
            }
            materialPath {
                moveTo(11.0f, 18.0f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(-4.58f, -4.59f)
                lineToRelative(4.58f, -4.59f)
                lineToRelative(-1.41f, -1.41f)
                lineToRelative(-6.0f, 6.0f)
                close()
            }
        }
        return _keyboardDoubleArrowLeft!!
    }

internal val KeyboardArrowLeft: ImageVector
    get() {
        if (_keyboardArrowLeft != null) {
            return _keyboardArrowLeft!!
        }
        _keyboardArrowLeft = materialIcon(
            name = "AutoMirrored.Filled.KeyboardArrowLeft", autoMirror
            = true
        ) {
            materialPath {
                moveTo(15.41f, 16.59f)
                lineTo(10.83f, 12.0f)
                lineToRelative(4.58f, -4.59f)
                lineTo(14.0f, 6.0f)
                lineToRelative(-6.0f, 6.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(1.41f, -1.41f)
                close()
            }
        }
        return _keyboardArrowLeft!!
    }

private var _keyboardArrowLeft: ImageVector? = null


private var _keyboardDoubleArrowLeft: ImageVector? = null
internal val KeyboardArrowRight: ImageVector
    get() {
        if (_keyboardArrowRight != null) {
            return _keyboardArrowRight!!
        }
        _keyboardArrowRight = materialIcon(
            name = "AutoMirrored.Filled.KeyboardArrowRight",
            autoMirror = true
        ) {
            materialPath {
                moveTo(8.59f, 16.59f)
                lineTo(13.17f, 12.0f)
                lineTo(8.59f, 7.41f)
                lineTo(10.0f, 6.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(-6.0f, 6.0f)
                lineToRelative(-1.41f, -1.41f)
                close()
            }
        }
        return _keyboardArrowRight!!
    }

private var _keyboardArrowRight: ImageVector? = null

internal val KeyboardDoubleArrowRight: ImageVector
    get() {
        if (_keyboardDoubleArrowRight != null) {
            return _keyboardDoubleArrowRight!!
        }
        _keyboardDoubleArrowRight = materialIcon(name = "Filled.KeyboardDoubleArrowRight") {
            materialPath {
                moveTo(6.41f, 6.0f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(4.58f, 4.59f)
                lineToRelative(-4.58f, 4.59f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(6.0f, -6.0f)
                close()
            }
            materialPath {
                moveTo(13.0f, 6.0f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(4.58f, 4.59f)
                lineToRelative(-4.58f, 4.59f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(6.0f, -6.0f)
                close()
            }
        }
        return _keyboardDoubleArrowRight!!
    }

private var _keyboardDoubleArrowRight: ImageVector? = null