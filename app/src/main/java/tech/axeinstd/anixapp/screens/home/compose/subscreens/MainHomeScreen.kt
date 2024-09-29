package tech.axeinstd.anixapp.screens.home.compose.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import tech.axeinstd.anilibri3.Libria
import tech.axeinstd.anilibri3.data.title.LPagination
import tech.axeinstd.anilibri3.data.title.LTitleList
import tech.axeinstd.anixapp.releaseScreenRoute
import tech.axeinstd.anixapp.view_models.PreloadTitleInfo

@Composable
fun MainHomeScreen(AniLibriaClient: Libria, sessionID: MutableState<String>, padding: PaddingValues, navController: NavController, preloadTitleInfo: PreloadTitleInfo) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    val loadTitlesCount = with (density) { screenHeight.toPx().toInt() / 120.dp.toPx().toInt() + 3}

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentPage = rememberSaveable { mutableIntStateOf(1) }

    val isLoading = rememberSaveable { mutableStateOf(false) }

    val titlesList = rememberSaveable {
        mutableStateOf(
            LTitleList(
                list = emptyList(),
                pagination = LPagination(
                    pages = 1,
                    current_page = 0,
                    items_per_page = 0,
                    total_items = 0
                )
            )
        )
    }

    val needLoadTitles by remember {
        derivedStateOf {
            (scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: 0) >= scrollState.layoutInfo.totalItemsCount - 2 && titlesList.value.pagination.pages > titlesList.value.pagination.current_page && !isLoading.value
        }
    }

    LaunchedEffect(needLoadTitles) {
        if (needLoadTitles || titlesList.value.list.isEmpty()) {
            coroutineScope.launch {
                isLoading.value = true
                val res: LTitleList = AniLibriaClient.advancedSearch(
                    "list",
                    page = currentPage.intValue,
                    filter = "id,names,description,posters",
                    items_per_page = loadTitlesCount,
                    order_by = "in_favorites",
                    sort_direction = 1
                )
                currentPage.intValue++
                titlesList.value = titlesList.value.copy(
                    list = titlesList.value.list + res.list,
                    pagination = res.pagination
                )
                isLoading.value = false
            }
        }
    }

    Scaffold { innerPadding ->
        val innerPaddingInScaffold = innerPadding
        LazyColumn(
            state = scrollState,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            item {
                Spacer(
                    modifier = Modifier.height(padding.calculateTopPadding() + 10.dp)
                )
            }
            items(titlesList.value.list) { title ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            preloadTitleInfo.releaseID.intValue = title.id ?: 9000
                            preloadTitleInfo.preloadTitleInfo.value = title
                            navController.navigate(releaseScreenRoute) {
                                popUpTo(releaseScreenRoute)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    val descriptionColor = MaterialTheme.colorScheme.surface.copy(
                        red = MaterialTheme.colorScheme.surface.red + if (isSystemInDarkTheme()) 0.5f else -0.5f,
                        green = MaterialTheme.colorScheme.surface.green + if (isSystemInDarkTheme()) 0.5f else -0.5f,
                        blue = MaterialTheme.colorScheme.surface.blue + if (isSystemInDarkTheme()) 0.5f else -0.5f
                    )
                    Row (
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SubcomposeAsyncImage(
                            model = title.posters?.getPosterUrl(),
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.1.toFloat())
                                        .background(MaterialTheme.colorScheme.background)
                                )
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("${title.names?.ru}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(title.description ?: "Описание не найдено", fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 11.sp, color = descriptionColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (isLoading.value) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}