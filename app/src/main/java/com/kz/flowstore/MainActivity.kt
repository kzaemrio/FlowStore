package com.kz.flowstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kz.flowstore.annotation.FlowStore
import com.kz.flowstore.ui.theme.FlowStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: MainViewModel by viewModels()

        setContent {
            FlowStoreTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Content(
                        viewModel.flow.collectAsState().value,
                        viewModel::reverseText,
                        viewModel::incCount,
                        viewModel::decCount,
                    )
                }
            }
        }
    }

    @Composable
    fun Content(
        uiState: UiState,
        reverseText: () -> Unit,
        incCount: () -> Unit,
        decCount: () -> Unit
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "text = ${uiState.text}")
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "count = ${uiState.count}")
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = reverseText) {
                Text(text = "reverse text")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = incCount) {
                Text(text = "inc count")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = decCount) {
                Text(text = "dec count")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@FlowStore
data class UiState(
    val text: String = "1234",
    val count: Int = 0,
    val list: List<List<Int>> = emptyList()
)
