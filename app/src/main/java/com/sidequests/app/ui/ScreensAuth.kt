package com.sidequests.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sidequests.app.model.SidequestsUiState

@Composable
fun AuthScreen(state: SidequestsUiState, viewModel: AppViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sidequests", style = MaterialTheme.typography.headlineLarge)
        Text(
            if (state.authModeSignUp) "Create your account" else "Sign in to continue",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        OutlinedTextField(
            value = state.authEmail,
            onValueChange = viewModel::setAuthEmail,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.authPassword,
            onValueChange = viewModel::setAuthPassword,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        state.authError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
        Button(
            onClick = viewModel::submitAuth,
            enabled = !state.authLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (state.authLoading) CircularProgressIndicator() else Text(if (state.authModeSignUp) "Create account" else "Sign in")
        }
        OutlinedButton(
            onClick = viewModel::toggleAuthMode,
            enabled = !state.authLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(if (state.authModeSignUp) "I already have an account" else "Create a new account")
        }
    }
}
