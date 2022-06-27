package explode.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

fun main() =
	singleWindowApplication(title = "Composable Explode", state = WindowState(size = DpSize(800.dp, 800.dp))) {
		MainComp()
	}

@Composable
@Preview
fun MainComp() {
	MaterialTheme {
		Column {
			Column(
				Modifier.fillMaxWidth().weight(1F).background(MaterialTheme.colors.background)
			) {
				StoreViewList()
			}
			NavBar()
		}
	}
}

@Composable
fun NavBar() {
	Row(
		Modifier.height(84.dp).padding(16.dp, 0.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		NavItem(Icons.Default.Home, "Home", Color.Gray)
		NavItem(Icons.Default.Create, "Test", Color.Gray)
		NavItem(Icons.Default.List, "Test", Color.Gray)
	}
}

@Composable
fun RowScope.NavItem(icon: ImageVector, desc: String, tint: Color) {
	Button(
		onClick = {},
		modifier = Modifier.weight(1F).fillMaxHeight(),
		shape = RectangleShape,
		colors = ButtonDefaults.outlinedButtonColors()
	) {
		Icon(icon, desc, Modifier.size(24.dp).weight(1F), tint)
	}
}