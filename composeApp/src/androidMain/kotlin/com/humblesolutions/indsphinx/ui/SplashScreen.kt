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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.humblesolutions.indsphinx.SplashDestination
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendUserProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

private val SplashTopColor = Color(0xFF2A3080)
private val SplashBottomColor = Color(0xFF7B90C8)

@Composable
fun SplashScreen(onSplashComplete: (SplashDestination) -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userProfileRepo = BackendUserProfileRepository()
            val isEnabled = try { userProfileRepo.isUserEnabled(currentUser.uid) } catch (e: Exception) { true }
            if (!isEnabled) {
                // Disabled-account cold start: full cleanup so the device
                // stops receiving pushes targeting this user.
                AndroidAuthRepository().signOutAndClearFcm()
                onSplashComplete(SplashDestination.NOT_LOGGED_IN)
                return@LaunchedEffect
            }
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                userProfileRepo.updateFcmToken(currentUser.uid, token)
            } catch (_: Exception) {}
            // The agreement covers a specific flat, so it can only be presented
            // once one is assigned. Treating "no flat" as nothing-to-sign sends
            // the occupant to Home instead of trapping them on a form that
            // cannot be built.
            val needsAgreement = try {
                val profile = userProfileRepo.getProfile(currentUser.uid)
                !profile.hasAcceptedAgreement && profile.flatId.isNotBlank()
            } catch (_: Exception) { false }
            onSplashComplete(if (needsAgreement) SplashDestination.NEEDS_AGREEMENT else SplashDestination.HOME)
        } else {
            onSplashComplete(SplashDestination.NOT_LOGGED_IN)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SplashTopColor, SplashBottomColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.humblesolutions.indsphinx.R.drawable.app_logo),
                    contentDescription = "My Nest Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "MY NEST",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "By Ind-Sphinx",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "RESIDENTIAL MAINTENANCE MANAGEMENT",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}
