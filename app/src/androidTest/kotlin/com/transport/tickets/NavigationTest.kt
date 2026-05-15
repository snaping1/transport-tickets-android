package com.transport.tickets

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun authScreen_showsLoginForm() {
        // Auth screen should be visible when not logged in
        composeRule.onNodeWithText("Вход").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Пароль").assertIsDisplayed()
    }

    @Test
    fun authScreen_switchToRegisterMode() {
        composeRule.onNodeWithText("Нет аккаунта? Зарегистрироваться").performClick()
        composeRule.onNodeWithText("Регистрация").assertIsDisplayed()
        composeRule.onNodeWithText("Зарегистрироваться").assertIsDisplayed()
    }

    @Test
    fun authScreen_loginButtonDisabledWhenFieldsEmpty() {
        composeRule.onNodeWithText("Войти").assertIsNotEnabled()
    }

    @Test
    fun authScreen_loginButtonEnabledWhenFieldsFilled() {
        composeRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeRule.onNodeWithText("Пароль").performTextInput("password123")
        composeRule.onAllNodesWithText("Войти").onFirst().assertIsEnabled()
    }
}
