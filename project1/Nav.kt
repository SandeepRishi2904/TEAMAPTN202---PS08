package com.example.project1

import HomeScreen
import SignUpScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Nav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController)
        }
        composable(route = "signup") {
            SignUpScreen(navController)
        }
        composable("home") {
            HomeScreen(navController)
        }
        composable("admin") {
            Admin(navController)
        }
        composable("staff") {
            Staff(navController)
        }
        composable("approve") {
            Approvers(navController)
        }

        composable("dept") {
            Dept(navController)
        }
    }
}
