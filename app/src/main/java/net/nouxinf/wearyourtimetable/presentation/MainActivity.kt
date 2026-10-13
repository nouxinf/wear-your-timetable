/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package net.nouxinf.wearyourtimetable.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

val dayList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

val Context.dataStore by preferencesDataStore(name = "timetable")

object Keys {
    val MONDAY = stringPreferencesKey("monday")
    val TUESDAY = stringPreferencesKey("tuesday")
    val WEDNESDAY = stringPreferencesKey("wednesday")
    val THURSDAY = stringPreferencesKey("thursday")
    val FRIDAY = stringPreferencesKey("friday")

    val DAYS = listOf (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
}

class TimetableRepository(private val context: Context) {
    private val defaults = listOf(
        "Maths 9:00",
        "Physics 10:00",
        "English 11:00",
        "Social Studies 12:00",
        "PE 13:00",
    )

    val week: Flow<List<String>> = context.dataStore.data.map { prefs -> Keys.DAYS.mapIndexed { i, key -> prefs[key] ?: defaults[i] } }

    suspend fun setDay(dayIndex: Int, text: String) {
        context.dataStore.edit { prefs -> prefs[Keys.DAYS[dayIndex]] = text }
    }
    suspend fun seedDefaults() {
        context.dataStore.edit { prefs ->
            Keys.DAYS.forEachIndexed { i, key ->
                if (key !in prefs) prefs[key] = defaults[i]
            }
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
    val week by repo.week.collectAsState(initial = List(5) { "" })

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
                        Text(text = week[page])
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
    var text by remember { mutableStateOf("") }

    // Runs once when the screen opens: load the saved text into the box
    LaunchedEffect(Unit) {
        text = repo.week.first()[dayIndex]
    }
    val save: () -> Unit = {
        scope.launch {
            repo.setDay(dayIndex, text.trim())
            onDone()
        }
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = dayList[dayIndex])
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = save) { Text("Save") }
        }
    }
}



@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp()
}