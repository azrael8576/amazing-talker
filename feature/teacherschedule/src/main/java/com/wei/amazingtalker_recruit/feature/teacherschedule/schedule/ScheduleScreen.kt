package com.wei.amazingtalker_recruit.feature.teacherschedule.schedule

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wei.amazingtalker_recruit.core.designsystem.ui.management.states.topappbar.FixedScrollFlagState
import com.wei.amazingtalker_recruit.core.designsystem.ui.management.states.topappbar.TopAppBarState
import com.wei.amazingtalker_recruit.core.designsystem.ui.management.states.topappbar.scrollflags.EnterAlwaysState
import com.wei.amazingtalker_recruit.core.designsystem.ui.theme.AtTheme
import com.wei.amazingtalker_recruit.core.model.data.IntervalScheduleTimeSlot
import com.wei.amazingtalker_recruit.feature.teacherschedule.R
import com.wei.amazingtalker_recruit.feature.teacherschedule.schedule.ui.DateTabLayout
import com.wei.amazingtalker_recruit.feature.teacherschedule.schedule.ui.ScheduleListPreviewParameterProvider
import com.wei.amazingtalker_recruit.feature.teacherschedule.schedule.ui.TimeItemHeader
import com.wei.amazingtalker_recruit.feature.teacherschedule.schedule.ui.TimeListHeader
import com.wei.amazingtalker_recruit.feature.teacherschedule.schedule.ui.TimeListItem
import com.wei.amazingtalker_recruit.feature.teacherschedule.scheduledetail.navigation.navigateToScheduleDetail
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

/**
 *
 * UI 事件決策樹
 * 下圖顯示了一個決策樹，用於查找處理特定事件用例的最佳方法。
 *
 *                                                      ┌───────┐
 *                                                      │ Start │
 *                                                      └───┬───┘
 *                                                          ↓
 *                                       ┌───────────────────────────────────┐
 *                                       │ Where is event originated?        │
 *                                       └──────┬─────────────────────┬──────┘
 *                                              ↓                     ↓
 *                                              UI                  ViewModel
 *                                              │                     │
 *                           ┌─────────────────────────┐      ┌───────────────┐
 *                           │ When the event requires │      │ Update the UI │
 *                           │ ...                     │      │ State         │
 *                           └─┬─────────────────────┬─┘      └───────────────┘
 *                             ↓                     ↓
 *                        Business logic      UI behavior logic
 *                             │                     │
 *     ┌─────────────────────────────────┐   ┌──────────────────────────────────────┐
 *     │ Delegate the business logic to  │   │ Modify the UI element state in the   │
 *     │ the ViewModel                   │   │ UI directly                          │
 *     └─────────────────────────────────┘   └──────────────────────────────────────┘
 *
 *
 */
private val MinToolbarHeight = 0.dp
private val MaxToolbarHeight = 160.dp

@Composable
internal fun ScheduleRoute(
    navController: NavController,
    tokenInvalidNavigate: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiStates: ScheduleViewState by viewModel.states.collectAsStateWithLifecycle()

    LaunchedEffect(uiStates.isTokenValid) {
        if (!uiStates.isTokenValid) {
            tokenInvalidNavigate()
        }
    }

    ScheduleScreen(
        uiStates = uiStates,
        onPreviousWeekClick = { viewModel.dispatch(ScheduleViewAction.UpdateWeek(WeekAction.PREVIOUS_WEEK)) },
        onNextWeekClick = { viewModel.dispatch(ScheduleViewAction.UpdateWeek(WeekAction.NEXT_WEEK)) },
        onWeekDateClick = { viewModel.dispatch(ScheduleViewAction.ShowSnackBar(message = listOf(it))) },
        onTabClick = { date, index ->
            viewModel.dispatch(
                ScheduleViewAction.SelectedTab(
                    date,
                    index
                )
            )
        },
        onListScroll = { viewModel.dispatch(ScheduleViewAction.ListScrolled) },
        onTimeSlotClick = { item -> navController.navigateToScheduleDetail(timeSlot = item) },
    )
}

/**
 * Here set toolbar scroll flag
 */
@Composable
private fun rememberToolbarState(toolbarHeightRange: IntRange): TopAppBarState {
    return rememberSaveable(saver = EnterAlwaysState.Saver) {
        EnterAlwaysState(toolbarHeightRange)
    }
}

@Composable
internal fun ScheduleScreen(
    uiStates: ScheduleViewState,
    onPreviousWeekClick: () -> Unit,
    onNextWeekClick: () -> Unit,
    onWeekDateClick: (String) -> Unit,
    onTabClick: (OffsetDateTime, Int) -> Unit,
    onListScroll: () -> Unit,
    onTimeSlotClick: (IntervalScheduleTimeSlot) -> Unit,
) {

    val toolbarHeightRange = with(LocalDensity.current) {
        MinToolbarHeight.roundToPx()..MaxToolbarHeight.roundToPx()
    }
    val toolbarState = rememberToolbarState(toolbarHeightRange)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                toolbarState.scrollTopLimitReached =
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                toolbarState.scrollOffset = toolbarState.scrollOffset - available.y
                return Offset(0f, toolbarState.consumed)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (available.y > 0) {
                    scope.launch {
                        animateDecay(
                            initialValue = toolbarState.height + toolbarState.offset,
                            initialVelocity = available.y,
                            animationSpec = FloatExponentialDecaySpec()
                        ) { value, _ ->
                            toolbarState.scrollTopLimitReached =
                                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                            toolbarState.scrollOffset =
                                toolbarState.scrollOffset - (value - (toolbarState.height + toolbarState.offset))
                            if (toolbarState.scrollOffset == 0f) scope.coroutineContext.cancelChildren()
                        }
                    }
                }

                return super.onPostFling(consumed, available)
            }
        }
    }

    if (uiStates.isTokenValid) {
        /**
         * Add the nested scroll connection to your top level @Composable element
         * using the nestedScroll modifier.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            ScheduleList(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
                    .graphicsLayer { translationY = toolbarState.height + toolbarState.offset }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { scope.coroutineContext.cancelChildren() }
                        )
                    },
                timeListUiState = uiStates.timeListUiState,
                listState = listState,
                contentPadding = PaddingValues(bottom = if (toolbarState is FixedScrollFlagState) MinToolbarHeight else 0.dp),
                isScrollInProgress = uiStates.isScrollInProgress,
                onListScroll = onListScroll,
                onTimeSlotClick = onTimeSlotClick,
            )
            ScheduleToolbar(
                progress = toolbarState.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { toolbarState.height.toDp() })
                    .graphicsLayer { translationY = toolbarState.offset },
                uiStates = uiStates,
                onPreviousWeekClick = onPreviousWeekClick,
                onNextWeekClick = onNextWeekClick,
                onWeekDateClick = onWeekDateClick,
                onTabClick = onTabClick,
            )
            AnimateToolbarOffset(toolbarState, listState, toolbarHeightRange)
        }
    }
}

@Composable
internal fun ScheduleList(
    modifier: Modifier = Modifier,
    timeListUiState: TimeListUiState,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isScrollInProgress: Boolean = false,
    onListScroll: () -> Unit,
    onTimeSlotClick: (IntervalScheduleTimeSlot) -> Unit,
) {
    /**
     * 使用 rememberUpdatedState 用於確保在 Compose 函數中的 callback（例如：事件處理器）可以獲取到最新的狀態值。
     * 例如，你可能會在 Compose 函數中使用 rememberUpdatedState 來保存用於事件處理的狀態。
     */
    val uiStates by rememberUpdatedState(newValue = timeListUiState)

    LaunchedEffect(timeListUiState) {
        if (isScrollInProgress) {
            listState.scrollToItem(0)
            onListScroll()
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        when (timeListUiState) {
            is TimeListUiState.Success -> {
                item {
                    TimeListHeader()
                }

                (uiStates as TimeListUiState.Success).groupedTimeSlots.forEach { (duringDayType, timeSlots) ->
                    item {
                        TimeItemHeader(duringDayType)
                    }
                    itemsIndexed(timeSlots) { _, timeSlot ->
                        TimeListItem(
                            timeSlot = timeSlot,
                            onTimeSlotClick = { onTimeSlotClick(it) },
                        )
                    }
                }
            }

            is TimeListUiState.Loading -> item {
            }

            is TimeListUiState.Error -> item {
            }

        }
    }
}

@Composable
private fun ScheduleToolbar(
    modifier: Modifier = Modifier,
    progress: Float,
    uiStates: ScheduleViewState,
    onPreviousWeekClick: () -> Unit,
    onNextWeekClick: () -> Unit,
    onWeekDateClick: (String) -> Unit,
    onTabClick: (OffsetDateTime, Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Column {
            WeekActionBar(
                uiStates = uiStates,
                onPreviousWeekClick = onPreviousWeekClick,
                onNextWeekClick = onNextWeekClick,
                onWeekDateClick = onWeekDateClick,
            )
            WeekActionBarBottom(
                selectedIndex = uiStates.selectedIndex,
                tabs = uiStates.dateTabs,
                onTabClick = onTabClick
            )
        }
    }
}

@Composable
fun WeekActionBar(
    uiStates: ScheduleViewState,
    onPreviousWeekClick: () -> Unit,
    onNextWeekClick: () -> Unit,
    onWeekDateClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val styledAttributes =
        context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
    val actionBarSizePx = styledAttributes.getDimensionPixelSize(0, 0)
    val density = context.resources.displayMetrics.density
    val actionBarSizeDp = actionBarSizePx / density

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(actionBarSizeDp.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (uiStates.isAvailablePreviousWeek) {
                        onPreviousWeekClick()
                    }
                },
                modifier = Modifier
                    .padding(start = 13.dp)
            ) {
                Image(
                    painterResource(id = R.drawable.arrow_left_gray),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.toolbar_item_icon)),
                    colorFilter = if (uiStates.isAvailablePreviousWeek) ColorFilter.tint(
                        MaterialTheme.colorScheme.primary
                    ) else null,
                )
            }

            val weekDate = uiStates.weekDateText
            TextButton(
                modifier = Modifier
                    .weight(1f),
                onClick = {
                    onWeekDateClick("開啟日曆選單: $weekDate")
                },
            ) {
                Text(
                    text = weekDate,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = onNextWeekClick,
                modifier = Modifier
                    .padding(start = 13.dp)
            ) {
                Image(
                    painterResource(id = R.drawable.arrow_right_gray),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.toolbar_item_icon)),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun WeekActionBarBottom(
    selectedIndex: Int,
    tabs: MutableList<OffsetDateTime>,
    onTabClick: (OffsetDateTime, Int) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            DateTabLayout(
                modifier = Modifier.fillMaxSize(),
                selectedIndex = selectedIndex,
                tabs = tabs,
                onTabClick = onTabClick,
            )
        }
    }
}

enum class ToolbarStatus {
    Hidden, Visible
}

@Composable
fun AnimateToolbarOffset(
    topAppBarState: TopAppBarState,
    listState: LazyListState,
    toolbarHeightRange: IntRange,
) {
    val toolbarStatus = remember {
        derivedStateOf {
            deriveToolbarStatus(topAppBarState.scrollOffset, toolbarHeightRange)
        }
    }
    val isScrollInProgress = rememberUpdatedState(newValue = listState.isScrollInProgress)

    LaunchedEffect(topAppBarState, isScrollInProgress.value) {
        if (!isScrollInProgress.value) {
            when (toolbarStatus.value) {
                ToolbarStatus.Hidden -> {
                    animateTo(topAppBarState, toolbarHeightRange.last.toFloat())
                }

                ToolbarStatus.Visible -> {
                    animateTo(topAppBarState, toolbarHeightRange.first.toFloat())
                }
            }
        }
    }
}

private fun deriveToolbarStatus(scrollOffset: Float, toolbarHeightRange: IntRange): ToolbarStatus {
    val largeToolbarHalf = toolbarHeightRange.last / 2f

    return when {
        scrollOffset > largeToolbarHalf -> ToolbarStatus.Hidden
        else -> ToolbarStatus.Visible
    }
}

private suspend fun animateTo(topAppBarState: TopAppBarState, targetValue: Float) {
    animate(
        initialValue = topAppBarState.scrollOffset,
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
    ) { value, _ ->
        topAppBarState.scrollOffset = value
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleToolbarPreview() {
    AtTheme {
        ScheduleToolbar(
            modifier = Modifier.height(MaxToolbarHeight),
            progress = 0f,
            uiStates = ScheduleViewState(),
            onPreviousWeekClick = { },
            onNextWeekClick = { },
            onWeekDateClick = { },
            onTabClick = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleListPreview(
    @PreviewParameter(ScheduleListPreviewParameterProvider::class)
    timeListUiState: TimeListUiState,
) {
    AtTheme {
        ScheduleList(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
            timeListUiState = timeListUiState,
            listState = rememberLazyListState(),
            contentPadding = PaddingValues(bottom = 0.dp),
            isScrollInProgress = false,
            onListScroll = { },
            onTimeSlotClick = { },
        )
    }
}

