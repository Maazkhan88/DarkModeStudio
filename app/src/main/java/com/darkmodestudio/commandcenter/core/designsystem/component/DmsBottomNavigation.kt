package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme

enum class DmsNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    HOME("Home", Icons.Outlined.Home, "home"),
    PROJECTS("Projects", Icons.Outlined.Folder, "projects"),
    CREATE("Create", Icons.Default.Add, "create"),
    TASKS("Tasks", Icons.Outlined.CheckCircle, "execution"),
    UPDATES("Updates", Icons.Outlined.Notifications, "updates")
}

@Composable
fun DmsBottomNavigation(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(DmsRadii.ShapeR28)
                .background(DmsColors.Surface02) // #090909
                .border(BorderStroke(1.dp, DmsColors.White14), DmsRadii.ShapeR28)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DmsNavItem.entries.forEach { item ->
                    if (item == DmsNavItem.CREATE) {
                        // Center 48x48dp circle #242424 with White64 plus
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DmsColors.White14)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onCreateClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = "Create",
                                tint = DmsColors.White64,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        val isSelected = selectedRoute == item.route
                        val itemBg = if (isSelected) DmsColors.SurfaceNavSelected else Color.Transparent
                        val itemColor = if (isSelected) DmsColors.White else DmsColors.White48

                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(itemBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onNavigate(item.route)
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = itemColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = item.label,
                                    style = DmsTheme.typography.caption.copy(
                                        fontSize = 10.sp,
                                        color = itemColor
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
