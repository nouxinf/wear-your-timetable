/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package net.nouxinf.wearyourtimetable.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.compose.*
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.nouxinf.wearyourtimetable.presentation.theme.WearYourTimetableTheme
import java.time.DayOfWeek
import java.time.LocalDate
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.PickerGroup
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.compose.material3.rememberPickerState
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}
@Serializable
data class Lesson(
    val subject: String,
    val room: String,
    val hour: Int = 8,
    val minute: Int = 30,
)
val dayList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

val Context.dataStore by preferencesDataStore(name = "timetable")

object Keys {
    val WEEK = stringPreferencesKey("week")
}


class TimetableRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val defaults: List<List<Lesson>> = listOf(
        listOf(Lesson("Maths", "A1", 10, 30), Lesson("Physics", "B2", 11, 20)),
        listOf(Lesson("English", "C3", 9, 30)),
        listOf(Lesson("Social Studies", "A4", 9, 30)),
        listOf(Lesson("PE", "Gym", 9, 30)),
        listOf(Lesson("Art", "D1", 9, 30)),
    )

    private fun decode(raw: String?): List<List<Lesson>> =
        raw?.let { runCatching { json.decodeFromString<List<List<Lesson>>>(it) }.getOrNull() }
            ?: defaults

    val week: Flow<List<List<Lesson>>> =
        context.dataStore.data.map { prefs -> decode(prefs[Keys.WEEK]) }

    suspend fun setDay(dayIndex: Int, lessons: List<Lesson>) {
        context.dataStore.edit { prefs ->
            val updated = decode(prefs[Keys.WEEK]).toMutableList()
            updated[dayIndex] = lessons
            prefs[Keys.WEEK] = json.encodeToString(updated)
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val repo = remember { TimetableRepository(context) }
    val navController = rememberSwipeDismissableNavController()

    // Kept here (above the screens) so the pager remembers its page when you return
    val pagerState = rememberPagerState(
        initialPage = startPage(),
        pageCount = { dayList.size }
    )

    WearYourTimetableTheme {
        AppScaffold {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "timetable"
            ) {
                composable("timetable") {
                    TimetableScreen(
                        repo = repo,
                        pagerState = pagerState,
                        onEdit = { day -> navController.navigate("edit/$day") }
                    )
                }
                composable("edit/{day}") { entry ->
                    val day = entry.arguments?.getString("day")?.toInt() ?: 0
                    EditScreen(
                        repo = repo,
                        dayIndex = day,
                        onDone = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun TimetableScreen(
    repo: TimetableRepository,
    pagerState: PagerState,
    onEdit: (Int) -> Unit
) {
    val week by repo.week.collectAsState(initial = List(5) { emptyList() })

    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(
            state = pagerState,
            flingBehavior = PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                ScreenScaffold {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Text(text = dayList[page])
                        Spacer(modifier = Modifier.height(8.dp))
                        week[page].forEach { lesson ->
                            Text("${formatTime(lesson.hour, lesson.minute)} ${lesson.subject} - ${lesson.room}")
                        }
                        Button(onClick = { onEdit(page) }) { Text("Edit") }
                    }
                }
            }
        }
    }
}

fun startPage(): Int {
    val today = LocalDate.now().dayOfWeek
    return if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) 0 else today.value - 1
}

@Composable
fun EditScreen(repo: TimetableRepository, dayIndex: Int, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val lessons = remember { mutableStateListOf<Lesson>() }
    val listState = rememberScalingLazyListState()
    // Index of the lesson whose time is being edited, or null when showing the list
    var pickingTimeFor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        lessons.clear()
        lessons.addAll(repo.week.first()[dayIndex])
    }

    // Swiping back from the time picker returns to the list instead of leaving the screen
    BackHandler(enabled = pickingTimeFor != null) { pickingTimeFor = null }

    val picking = pickingTimeFor
    if (picking != null) {
        val lesson = lessons[picking]
        TimePicker(
            initialTime = LocalTime.of(lesson.hour, lesson.minute),
            onTimePicked = { time ->
                lessons[picking] = lesson.copy(hour = time.hour, minute = time.minute)
                pickingTimeFor = null
            },
            timePickerType = TimePickerType.HoursMinutes24H,
        )
    } else {
        ScreenScaffold(scrollState = listState) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 10.dp),
            ) {
                item { Text(dayList[dayIndex]) }

                itemsIndexed(lessons) { index, lesson ->
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LessonField(
                            value = lesson.subject,
                            hint = "Subject",
                            imeAction = ImeAction.Next,
                            onChange = { lessons[index] = lessons[index].copy(subject = it) },
                        )
                        Button(onClick = { pickingTimeFor = index }) {
                            Text(formatTime(lesson.hour, lesson.minute))
                        }
                        LessonField(
                            value = lesson.room,
                            hint = "Room",
                            imeAction = ImeAction.Done,
                            onChange = { lessons[index] = lessons[index].copy(room = it) },
                        )
                        Button(onClick = { lessons.removeAt(index) }) { Text("Remove") }
                    }
                }

                item {
                    Button(onClick = { lessons.add(Lesson("", "", 8, 30)) }) { Text("Add lesson") }
                }

                item {
                    Button(onClick = {
                        scope.launch {
                            val cleaned = lessons
                                .map { it.copy(subject = it.subject.trim(), room = it.room.trim()) }
                                .filter { it.subject.isNotEmpty() }
                                .sortedWith(compareBy({ it.hour }, { it.minute }))
                            repo.setDay(dayIndex, cleaned)
                            onDone()
                        }
                    }) { Text("Save") }
                }
            }
        }
    }
}

fun formatTime(hour: Int, minute: Int) = "%02d:%02d".format(hour, minute)

@Composable
private fun LessonField(
    value: String,
    hint: String,
    imeAction: ImeAction,
    onChange: (String) -> Unit,
) {
    var text by remember { mutableStateOf(value) }

    // If the list changes the value from outside (e.g. a lesson above was removed), follow it
    LaunchedEffect(value) {
        if (value != text) text = value
    }

    BasicTextField(
        value = text,
        onValueChange = {
            val cleaned = it.replace("\n", "")
            text = cleaned
            onChange(cleaned)
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (text.isEmpty()) {
                    Text(text = hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                innerTextField()
            }
        },
    )
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp()
}