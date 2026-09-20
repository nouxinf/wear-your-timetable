/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package net.nouxinf.wearyourtimetable.presentation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

@Composable
fun WearApp( navigateBack: () -> Unit) {
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
                                Text(text = "Swipe left and right")
                                if (page == 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = navigateBack) { Text("Exit") }
                                }
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