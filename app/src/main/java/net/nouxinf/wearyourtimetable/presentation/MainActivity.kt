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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.compose.*
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.nouxinf.wearyourtimetable.R
import net.nouxinf.wearyourtimetable.presentation.theme.WearYourTimetableTheme
import java.time.DayOfWeek
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp( navigateBack = { finish() })
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
fun WearApp( navigateBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { TimetableRepository(context) }
    val week by repo.week.collectAsState(initial = List(5) { "" })

    LaunchedEffect(Unit) { repo.seedDefaults() }
    WearYourTimetableTheme {
        AppScaffold {
            val today = LocalDate.now().dayOfWeek
            val startPage = when (today) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 0   // fall back to Monday on weekends
                else -> today.value - 1                      // MONDAY = 1, so subtract 1
            }

            val pagerState = rememberPagerState(
                initialPage = startPage,
                pageCount = { dayList.size }
            )

            HorizontalPagerScaffold(pagerState = pagerState) {
                HorizontalPager(
                    state = pagerState,
                    flingBehavior =
                        PagerScaffoldDefaults.snapWithSpringFlingBehavior(
                            state = pagerState
                        ),
                ) { page ->
                    AnimatedPage(pageIndex = page, pagerState = pagerState) {
                        ScreenScaffold {
                            Column(
                                modifier = Modifier.fillMaxSize()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                            ) {
                                Text(text = dayList[page])
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = week[page])
                                /*if (page == 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = navigateBack) { Text("Exit") }
                                }*/
                                Button(onClick = {}) { Text ("Edit")}
                            }
                        }
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp(navigateBack = {})
}