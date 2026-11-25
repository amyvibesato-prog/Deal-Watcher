package com.example

import com.example.network.EpicApiService
import com.example.network.SteamApiService
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun testSteamTargetResolution_steam64Numeric() = runBlocking {
        val service = SteamApiService()
        val result = service.resolveSteamTarget("76561198012345678")
        assertNotNull(result)
        assertEquals("76561198012345678", result?.steam64Id)
    }

    @Test
    fun testSteamTargetResolution_friendCodeConversion() = runBlocking {
        val service = SteamApiService()
        // Friend code: 12345678 -> 76561197960265728 + 12345678 = 76561197972611406
        val result = service.resolveSteamTarget("12345678")
        assertNotNull(result)
        assertEquals("76561197972611406", result?.steam64Id)
    }

    @Test
    fun testSteamTargetResolution_friendCodeWithFormatting() = runBlocking {
        val service = SteamApiService()
        // Friend code with dashes or spaces: "123-456-78"
        val result = service.resolveSteamTarget("123-456-78")
        assertNotNull(result)
        assertEquals("76561197972611406", result?.steam64Id)
    }

    @Test
    fun testSteamTargetResolution_profileUrl() = runBlocking {
        val service = SteamApiService()
        val result = service.resolveSteamTarget("https://steamcommunity.com/profiles/76561198012345678/")
        assertNotNull(result)
        assertEquals("76561198012345678", result?.steam64Id)
    }

    @Test
    fun testTestConnection_detectsEmail() = runBlocking {
        val service = SteamApiService()
        val status = service.testSteamConnection("user@gmail.com", "")
        assertFalse(status.success)
        assertEquals("EMAIL_ENTERED", status.errorCode)
    }

    @Test
    fun testEpicSlugExtraction_ignoresHexHashUrlSlugAndUsesOfferMappings() {
        val epicService = EpicApiService()

        val jsonItem = JSONObject().apply {
            put("title", "Model Builder: Complete Edition")
            put("urlSlug", "3f18c54a6e00424ba6e8f4422167cac7") // 32-char hex hash that causes 404
            put("offerMappings", JSONArray().apply {
                put(JSONObject().apply {
                    put("pageSlug", "model-builder-complete-edition-f9b456")
                    put("pageType", "productHome")
                })
            })
        }

        val slug = epicService.extractProductSlug(jsonItem)
        assertEquals("model-builder-complete-edition-f9b456", slug)
    }

    @Test
    fun testEpicSlugExtraction_cleansHomeSuffix() {
        val epicService = EpicApiService()

        val jsonItem = JSONObject().apply {
            put("title", "Them's Fightin' Herds")
            put("productSlug", "thems-fightin-herds/home")
        }

        val slug = epicService.extractProductSlug(jsonItem)
        assertEquals("thems-fightin-herds", slug)
    }
}
