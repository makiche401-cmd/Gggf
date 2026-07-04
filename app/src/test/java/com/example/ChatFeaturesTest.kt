package com.example

import com.example.ui.screens.findOtp
import com.example.ui.screens.findFirstUrl
import com.example.ui.screens.getLinkPreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatFeaturesTest {

    @Test
    fun `test OTP detection`() {
        // Numeric OTP detection
        assertEquals("123456", findOtp("Your OTP code is 123456. Do not share!"))
        assertEquals("9876", findOtp("Verification pin: 9876"))
        
        // Alphanumeric code detection with keywords
        assertEquals("SECURE45", findOtp("Your code is SECURE45!"))
        
        // No OTP should be found
        assertNull(findOtp("Hello how are you?"))
    }

    @Test
    fun `test URL extraction`() {
        assertEquals("https://google.com", findFirstUrl("Check this out: https://google.com for more info"))
        assertEquals("http://test.org/abc?q=1", findFirstUrl("Visit http://test.org/abc?q=1 now!"))
        assertNull(findFirstUrl("No links here!"))
    }

    @Test
    fun `test link preview verification`() {
        // Google is verified
        val googlePreview = getLinkPreviewData("https://google.com")
        assertTrue(googlePreview.isVerified)
        assertEquals("google.com", googlePreview.domain)
        
        // YouTube is verified
        val ytPreview = getLinkPreviewData("https://www.youtube.com/watch?v=123")
        assertTrue(ytPreview.isVerified)
        assertEquals("youtube.com", ytPreview.domain)

        // Random domain is unverified
        val untrustedPreview = getLinkPreviewData("https://unverified-domain.xyz/phishing")
        assertFalse(untrustedPreview.isVerified)
        assertEquals("unverified-domain.xyz", untrustedPreview.domain)
    }
}
