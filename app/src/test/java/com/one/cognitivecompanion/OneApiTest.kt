package com.one.cognitivecompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneApiTest {
    @Test
    fun backendRolesMapToTheProductExperiences() {
        assertEquals(OneRole.RESIDENT, "resident".toOneRole())
        assertEquals(OneRole.CAREGIVER, "admin".toOneRole())
        assertEquals(OneRole.CAREGIVER, "caregiver".toOneRole())
        assertEquals(OneRole.PUBLISHER, "publisher".toOneRole())
    }

    @Test
    fun roleWireValuesMatchTheBackendContract() {
        assertEquals("resident", OneRole.RESIDENT.wireValue)
        assertEquals("caregiver", OneRole.CAREGIVER.wireValue)
        assertEquals("publisher", OneRole.PUBLISHER.wireValue)
    }

    @Test
    fun runtimeConfigurationIdentifiesTheEmulatorDemoEndpoint() {
        assertTrue(RuntimeConfiguration("http://10.0.2.2:8000/api/v1").isDemoEndpoint)
        assertTrue(RuntimeConfiguration("http://127.0.0.1:8000/api/v1").isDemoEndpoint)
        assertFalse(RuntimeConfiguration("https://one-api.example.ts.net/api/v1").isDemoEndpoint)
    }
}
