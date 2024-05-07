import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import data.domain.models.CurrencyRate
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.MainViewModel
import ui.UiState

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Navigator(CurrencyScreen())
        }
    }
}

class CurrencyScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { MainViewModel() }
        val state = viewModel.currencyRatesUiState.collectAsState().value
        CurrencyRatesScreen(state, viewModel::convertCurrency)
    }
}

@Composable
fun CurrencyRatesScreen(
    state: UiState<List<CurrencyRate>>,
    onAmountChange: (Double, String) -> Unit
) {
    Column(modifier = Modifier) {
        CurrencyRatesContent(state) { amount, currencyCode ->
            onAmountChange(amount, currencyCode)
        }
    }
}

@Composable
fun CurrencyRatesContent(
    uiState: UiState<List<CurrencyRate>>,
    onAmountChange: (Double, String) -> Unit
) {
    when (uiState) {
        is UiState.Success -> {
            CurrencyRateUi(uiState.data, onAmountChange)
        }

        is UiState.Loading -> {
            ShowLoading()
        }

        is UiState.Error -> {
            ShowError(text = uiState.message)
        }
    }
}

@Composable
fun CurrencyRateUi(list: List<CurrencyRate>, onAmountChange: (Double, String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        var text by remember { mutableStateOf("") }
        val initialIndex = list.indexOfFirst { it.currencyCode == "USD" }
        var selectedIndex by remember { mutableIntStateOf(initialIndex) }
        OutlinedTextField(
            value = text,
            label = { Text("Amount") },
            onValueChange = {
                text = it
                onAmountChange(text.stringToDouble(), list[selectedIndex].currencyCode)
            },
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.size(20.dp))
        CurrencyDropdown(
            label = "Currency",
            items = list,
            selectedIndex = selectedIndex,
            onItemSelected = { index, _ ->
                selectedIndex = index
                onAmountChange(text.stringToDouble(), list[selectedIndex].currencyCode)
            },
            modifier = Modifier
                .wrapContentHeight()
        )
    }
    ExchangeRatesList(list = list)
}

@Composable
private fun ExchangeRatesList(list: List<CurrencyRate>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .padding(16.dp)
    ) {
        items(list.size) { index ->
            CurrencyItem(data = list[index])
        }
    }
}

@Composable
fun CurrencyItem(data: CurrencyRate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(2.dp, Color.Gray),
                shape = RoundedCornerShape(8.dp)
            ).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween) {
            CurrencyCode(code = data.currencyCode)
            CurrencyName(name = data.currencyName)
        }
        ExchangeRate(rate = data.baseRate.toString())
    }

}

@Composable
fun CurrencyCode(code: String) {
    if (code.isNotEmpty()) {
        Text(
            text = code,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
fun CurrencyName(name: String) {
    if (name.isNotEmpty()) {
        Text(
            text = name,
            fontWeight = FontWeight.Light,
            color = Color.Gray,
            maxLines = 1,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
fun ExchangeRate(rate: String?) {
    if (!rate.isNullOrEmpty()) {
        Text(
            text = rate,
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
fun <T> CurrencyDropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String,
    notSetLabel: String? = null,
    items: List<T>,
    selectedIndex: Int = -1,
    onItemSelected: (index: Int, item: T) -> Unit,
    selectedItemToString: (T) -> String = { it.toString() },
    drawItem: @Composable (T, Boolean, Boolean, () -> Unit) -> Unit = { item, selected, itemEnabled, onClick ->
        LargeDropdownMenuItem(
            text = item.toString(),
            selected = selected,
            enabled = itemEnabled,
            onClick = onClick,
        )
    },
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.height(IntrinsicSize.Min)) {
        OutlinedTextField(
            label = { Text(label) },
            value = items.getOrNull(selectedIndex)?.let { selectedItemToString(it) } ?: "",
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                val icon = if(expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                Icon(icon, "")
            },
            onValueChange = { },
            readOnly = true,
        )

        // Transparent clickable surface on top of OutlinedTextField
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .clickable(enabled = enabled) { expanded = true },
            color = Color.Transparent,
        ) {}
    }

    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
        ) {
            MaterialTheme {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                ) {
                    val listState = rememberLazyListState()
                    if (selectedIndex > -1) {
                        LaunchedEffect("ScrollToSelected") {
                            listState.scrollToItem(index = selectedIndex)
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
                        if (notSetLabel != null) {
                            item {
                                LargeDropdownMenuItem(
                                    text = notSetLabel,
                                    selected = false,
                                    enabled = false,
                                    onClick = { },
                                )
                            }
                        }
                        itemsIndexed(items) { index, item ->
                            val selectedItem = index == selectedIndex
                            drawItem(
                                item,
                                selectedItem,
                                true
                            ) {
                                onItemSelected(index, item)
                                expanded = false
                            }

                            if (index < items.lastIndex) {
                                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LargeDropdownMenuItem(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colors.onSurface.copy(alpha = 0.38F)
        selected -> MaterialTheme.colors.primary.copy(alpha = 1F)
        else -> MaterialTheme.colors.onSurface.copy(alpha = 1F)
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(modifier = Modifier
            .clickable(enabled) { onClick() }
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.subtitle1,
                color = Color.Black,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

fun String.stringToDouble(): Double {
    return try {
        this.toDouble()
    } catch (e: Exception) {
        1.0
    }
}

@Composable
fun ShowLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        CircularProgressIndicator(modifier = Modifier
            .align(Alignment.Center)
            .semantics {
                contentDescription = "Loading"
            })
    }
}

@Composable
fun ShowError(text: String, retryClicked: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
    }
}