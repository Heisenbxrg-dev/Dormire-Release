import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.music.spotui.ui.components.AppBackdrop
import com.music.spotui.ui.components.DormireSplash
import com.music.spotui.ui.theme.SpotuiTheme

@Composable
fun DormireIosApp() {
    SpotuiTheme {
        var showSplash by remember { mutableStateOf(true) }
        Box(Modifier.fillMaxSize()) {
            AppBackdrop()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Dormire iOS", color = Color.White)
            }
            if (showSplash) {
                DormireSplash(onFinished = { showSplash = false })
            }
        }
    }
}
