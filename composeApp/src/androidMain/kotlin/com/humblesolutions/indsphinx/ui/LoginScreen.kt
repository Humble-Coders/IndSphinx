package com.humblesolutions.indsphinx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.humblesolutions.indsphinx.viewmodel.AuthUiState
import com.humblesolutions.indsphinx.viewmodel.AuthViewModel
import com.humblesolutions.indsphinx.viewmodel.PasswordResetUiState

private val NavyBlue = Color(0xFF1E2D6B)
private val BackgroundGray = Color(0xFFF2F4F8)
private val ErrorRed = Color(0xFFE53935)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: (needsAgreement: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val passwordResetState by viewModel.passwordResetState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        val s = uiState
        if (s is AuthUiState.Success) {
            onAuthSuccess(s.needsAgreement)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(48.dp))

            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NavyBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Apartment, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Welcome to Indsphinx",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = "Housing Management System", fontSize = 14.sp, color = Color(0xFF888888))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("Employee ID / Email", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (uiState is AuthUiState.Error) viewModel.resetState()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your employee ID or email", color = Color(0xFFAAAAAA)) },
                        leadingIcon = { Icon(Icons.Outlined.Email, null, tint = Color(0xFFAAAAAA)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("Password", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (uiState is AuthUiState.Error) viewModel.resetState()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your password", color = Color(0xFFAAAAAA)) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = Color(0xFFAAAAAA)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFFAAAAAA)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(
                            onClick = { viewModel.sendPasswordReset(email.trim()) },
                            enabled = !isLoading && passwordResetState !is PasswordResetUiState.Loading
                        ) {
                            if (passwordResetState is PasswordResetUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = NavyBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Forgot Password?", color = NavyBlue, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }
                    }

                    if (uiState is AuthUiState.Error) {
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = ErrorRed,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.signIn(email.trim(), password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        } else {
                            Text(
                                text = "Login  →",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Secure login powered by Indsphinx", color = Color(0xFF888888), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("© 2026 Indsphinx Accommodation System", color = Color(0xFF888888), fontSize = 12.sp)
            }

            Spacer(Modifier.height(48.dp))
        }

        when (val rs = passwordResetState) {
            is PasswordResetUiState.Success -> {
                AlertDialog(
                    onDismissRequest = { viewModel.clearPasswordResetState() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearPasswordResetState() }) {
                            Text("OK", color = NavyBlue, fontWeight = FontWeight.Medium)
                        }
                    },
                    title = { Text("Check your email") },
                    text = {
                        Text(
                            "If an account exists for that address, you'll receive instructions to reset your password."
                        )
                    }
                )
            }
            is PasswordResetUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.clearPasswordResetState() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearPasswordResetState() }) {
                            Text("OK", color = NavyBlue, fontWeight = FontWeight.Medium)
                        }
                    },
                    title = { Text("Reset password") },
                    text = { Text(rs.message) }
                )
            }
            else -> { }
        }
    }
}
