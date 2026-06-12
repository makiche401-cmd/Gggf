package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.FloatingHeartsBackground
import com.example.ui.components.VioraGradientButton
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// =============================================================================
// SCREEN 1: Welcome Screen
// =============================================================================
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
    ) {
        // Full screen ambient couples artwork (Subtle parallax/ambient blur styling)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://images.unsplash.com/photo-1543807535-eceef0bc6599?auto=format&fit=crop&q=80&w=600")
                .crossfade(true)
                .build(),
            contentDescription = "Romantic Couple background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.35f)
        )

        // Custom heart particle emitter back drop
        FloatingHeartsBackground(modifier = Modifier.fillMaxSize())

        // Top Gradient subtle overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkCharcoalBg.copy(alpha = 0.5f),
                            DarkCharcoalBg
                        ),
                        startY = 100f
                    )
                )
        )

        // Foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Viora",
                        tint = AccentCrimsonPink,
                        modifier = Modifier
                            .size(32.dp)
                            .purpleGlow()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Viora",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.purpleGlow()
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Tagline "Real people. Real connections."
                Text(
                    text = buildAnnotatedString {
                        append("Real people. Real ")
                        withStyle(style = SpanStyle(color = AccentCrimsonPink, fontWeight = FontWeight.Bold)) {
                            append("connections.")
                        }
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Glassmoprhic Callout box to match screenshots
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(
                        backgroundColor = Color(0x3FF140B1), 
                        borderColor = BorderColor,
                        borderRadius = 22.dp
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Account",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Start your journey to meaningful connections 💕",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Large envelope visual symbol
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x33B01DFF))
                        .border(1.dp, Color(0xFFB01DFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email sign up badge",
                        tint = BrightNeonPurple,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                VioraGradientButton(
                    text = "Create Account",
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_welcome_get_started",
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Login",
                        color = BrightNeonPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .testTag("welcome_login_link")
                            .clickable { onLoginClick() }
                    )
                }
            }

            // Forgot Password drawer / Security footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Secondary Password reset card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(backgroundColor = Color(0x1F140B1F), borderColor = BorderColor, borderRadius = 16.dp)
                        .clickable { onForgotPasswordClick() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x1ABD1DFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock icon", tint = PrimaryPinkPurple, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Forgot Password?", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("No worries, we'll help you reset it.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Arrow", tint = TextSecondary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real Privacy Trust Statement
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security shielding",
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "We care about your privacy and data security.\nYour information is always protected.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}


// =============================================================================
// SCREEN 2: How Viora Works
// =============================================================================
@Composable
fun HowItWorksScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val slides = listOf(
        OnboardingSlide(
            step = "1",
            title = "Create Your Profile",
            description = "Add your photos, write a bio and share your interests.",
            illustration = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=400",
            pillText = "Sophia, 24",
            badges = listOf("Travel", "Music", "Coffee")
        ),
        OnboardingSlide(
            step = "2",
            title = "Discover Amazing People",
            description = "Explore profiles and find people who match your vibe.",
            illustration = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=400",
            pillText = "James, 25",
            badges = listOf("Fitness", "Design")
        ),
        OnboardingSlide(
            step = "3",
            title = "Like & Match",
            description = "Like someone? If they like you back, it's a match!",
            illustration = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?auto=format&fit=crop&q=80&w=400",
            pillText = "It's a Match!",
            badges = listOf("Mutual Like 💖")
        ),
        OnboardingSlide(
            step = "4",
            title = "Chat & Connect",
            description = "Start a conversation and build something real.",
            illustration = "https://images.unsplash.com/photo-1543807535-eceef0bc6599?auto=format&fit=crop&q=80&w=400",
            pillText = "Connected Now",
            badges = listOf("Instant Chat 💬")
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Onboarding",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${pagerState.currentPage + 1}/4",
                    color = BrightNeonPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Illustration Card with Glassmorphic overlay
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .glassmorphic(borderRadius = 24.dp)
                            .purpleGlow(radius = 15.dp)
                            .padding(8.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(slide.illustration)
                                .crossfade(true)
                                .build(),
                            contentDescription = slide.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )

                        // Float card pill matching screenshot style
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .fillMaxWidth()
                                .glassmorphic(backgroundColor = Color(0x99140B1F), borderRadius = 12.dp)
                                .padding(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(slide.pillText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    slide.badges.forEach { badge ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF2C163C), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(badge, color = TextSecondary, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Step indicator
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryPinkPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(slide.step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = slide.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = slide.description,
                        color = TextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Bottom Buttons & Page Indicator Dots
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(width = if (pagerState.currentPage == index) 24.dp else 8.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (pagerState.currentPage == index) PrimaryPinkPurple else BorderColor)
                        )
                    }
                }

                VioraGradientButton(
                    text = if (pagerState.currentPage == 3) "Proceed to Guide" else "Next Step",
                    onClick = {
                        if (pagerState.currentPage < 3) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onContinue()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

data class OnboardingSlide(
    val step: String,
    val title: String,
    val description: String,
    val illustration: String,
    val pillText: String,
    val badges: List<String>
)


// =============================================================================
// SCREEN 3: Messaging Guide & Safety
// =============================================================================
@Composable
fun MessagingGuideScreen(
    onLetGetStarted: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Why You'll Love Viora",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Core features matching guide layout screenshot 3
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
            ) {
                GuideTile(
                    icon = Icons.Default.Verified,
                    iconColor = SuccessGreen,
                    title = "Verified Profiles",
                    description = "We verify profiles to ensure you connect with real people, protected with manual review badges."
                )

                GuideTile(
                    icon = Icons.Default.AutoAwesome,
                    iconColor = BrightNeonPurple,
                    title = "Smart Matching",
                    description = "Our algorithm helps you find people who truly match your interests, hobbies, and relationship goals."
                )

                GuideTile(
                    icon = Icons.Default.Lock,
                    iconColor = PrimaryPinkPurple,
                    title = "Private & Safe",
                    description = "End-to-end privacy controls and secure messaging that encrypts transaction feeds and media files."
                )

                GuideTile(
                    icon = Icons.Default.FlashOn,
                    iconColor = AccentCrimsonPink,
                    title = "Fast & Easy",
                    description = "Beautiful design and smooth native experience to help you find local matches effortlessly."
                )

                GuideTile(
                    icon = Icons.Default.Forum,
                    iconColor = BrightNeonPurple,
                    title = "Build Real Connections",
                    description = "Not just matches, but meaningful, offline-first conversations, voice notes, and posts."
                )
            }

            // Bottom CTA
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VioraGradientButton(
                    text = "Let's Get Started",
                    onClick = onLetGetStarted,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_guide_lets_get_started"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = buildAnnotatedString {
                        append("By continuing, you agree to our ")
                        withStyle(style = SpanStyle(color = BrightNeonPurple, fontWeight = FontWeight.SemiBold)) {
                            append("Terms of Service")
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(color = BrightNeonPurple, fontWeight = FontWeight.SemiBold)) {
                            append("Privacy Policy")
                        }
                    },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun GuideTile(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Verified,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(backgroundColor = Color(0x1A140B1F), borderColor = BorderColor, borderRadius = 18.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f))
                .border(1.dp, iconColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
