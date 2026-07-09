package com.cosmin23.mediaplayer.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * Minimal, self-contained drag-and-drop reordering for a [androidx.compose.foundation.lazy.LazyColumn].
 * [onMove] is called with source/target indices as the dragged item crosses its neighbours; the
 * caller applies the swap to its own list (locally or on the player queue).
 */
@Composable
fun rememberDragDropState(lazyListState: LazyListState, onMove: (Int, Int) -> Unit): DragDropState =
    remember(lazyListState) { DragDropState(lazyListState, onMove) }

class DragDropState internal constructor(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    private var draggingDelta by mutableFloatStateOf(0f)
    private var draggingInitialOffset by mutableIntStateOf(0)

    private val draggingItemInfo: LazyListItemInfo?
        get() = draggingItemIndex?.let { index ->
            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }

    val draggingItemOffset: Float
        get() = draggingItemInfo?.let { draggingInitialOffset + draggingDelta - it.offset } ?: 0f

    internal fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also {
                draggingItemIndex = it.index
                draggingInitialOffset = it.offset
            }
    }

    internal fun onDrag(offset: Offset) {
        draggingDelta += offset.y
        val info = draggingItemInfo ?: return
        val current = draggingItemIndex ?: return
        val startOffset = info.offset + draggingItemOffset
        val middle = startOffset + info.size / 2f
        val target = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            middle.toInt() in item.offset..(item.offset + item.size) && item.index != current
        }
        if (target != null) {
            onMove(current, target.index)
            draggingItemIndex = target.index
        }
    }

    internal fun onDragInterrupted() {
        draggingItemIndex = null
        draggingDelta = 0f
        draggingInitialOffset = 0
    }
}

/** Attach to the LazyColumn to start reordering on long-press. */
fun Modifier.dragContainer(state: DragDropState): Modifier = pointerInput(state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { state.onDragStart(it) },
        onDragEnd = { state.onDragInterrupted() },
        onDragCancel = { state.onDragInterrupted() },
        onDrag = { change, amount ->
            change.consume()
            state.onDrag(amount)
        }
    )
}

/** Wrap each LazyColumn item so the dragged one floats above and the rest animate into place. */
@Composable
fun LazyItemScope.DraggableItem(
    state: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val dragging = index == state.draggingItemIndex
    val itemModifier = if (dragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = state.draggingItemOffset }
    } else {
        Modifier.animateItem()
    }
    androidx.compose.foundation.layout.Box(modifier.then(itemModifier)) { content(dragging) }
}
