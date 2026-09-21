package com.sidequests.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sidequests.app.ui.PrimaryButton
import com.sidequests.app.ui.theme.DiscoveryTeal
import com.sidequests.app.ui.theme.ExplorerIndigo

@Composable
fun AuthScreen(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var createAccount by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "SIDEQUESTS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = ExplorerIndigo,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (createAccount) {
                "Create your explorer profile"
            } else {
                "Turn free time into your next small adventure"
            },
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(28.dp))

        if (createAccount) {
            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    onClearFeedback()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                enabled = !state.isBusy,
            )

            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                onClearFeedback()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !state.isBusy,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                onClearFeedback()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !state.isBusy,
        )

        state.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        state.message?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = DiscoveryTeal,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = if (createAccount) "Create account" else "Sign in",
            onClick = {
                if (createAccount) {
                    onSignUp(displayName, email, password)
                } else {
                    onSignIn(email, password)
                }
            },
            enabled = !state.isBusy,
        )

        if (state.isBusy) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        TextButton(
            onClick = {
                createAccount = !createAccount
                onClearFeedback()
            },
            enabled = !state.isBusy,
        ) {
            Text(
                if (createAccount) {
                    "Already have an account? Sign in"
                } else {
                    "New to Sidequests? Create an account"
                }
            )
        }
    }
}

@Composable
fun MissingSupabaseConfigurationScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Backend configuration missing",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Provide SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY through local.properties or environment variables, then rebuild the app.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
