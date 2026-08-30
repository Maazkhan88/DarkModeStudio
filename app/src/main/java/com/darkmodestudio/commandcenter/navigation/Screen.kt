package com.darkmodestudio.commandcenter.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ConnectStack : Screen("connect_stack")
    object Projects : Screen("projects")
    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(projectId: String) = "project_detail/$projectId"
    }
    object Agents : Screen("agents")
    object PlatformHealth : Screen("health")
    object Execution : Screen("execution")
    object Updates : Screen("updates")
    object Settings : Screen("settings")
}
