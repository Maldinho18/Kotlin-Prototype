package com.sidequests.app.sensor.camera

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoProofFileFactoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `each capture gets a distinct JPEG target without replacing earlier evidence`() {
        val factory = JpegPhotoProofFileFactory(temporaryFolder.root)
        val first = factory.create("botanical-garden", 3)
        first.file.writeText("previous photo")
        val retry = factory.create("botanical-garden", 3)
        val otherStep = factory.create("botanical-garden", 1)

        assertNotEquals(first.file, retry.file)
        assertNotEquals(first.file, otherStep.file)
        assertEquals("previous photo", first.file.readText())
        assertEquals(3, retry.stepIndex)
        assertTrue(retry.file.name.startsWith("step-4-"))
        assertTrue(otherStep.file.name.startsWith("step-2-"))
        assertEquals("jpg", retry.file.extension)
        assertTrue(retry.file.isFile)
    }

    @Test
    fun `quest IDs cannot escape the private evidence directory`() {
        val root = File(temporaryFolder.root, "quest-proofs").canonicalFile
        val factory = JpegPhotoProofFileFactory(temporaryFolder.root)
        for (id in listOf("../../outside", "..\\outside", "...", "", "/absolute/path")) {
            val proof = factory.create(id, 0)
            assertEquals(root, proof.file.canonicalFile.parentFile?.parentFile)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid step is rejected before creating evidence`() {
        JpegPhotoProofFileFactory(temporaryFolder.root).create("botanical-garden", -1)
    }

    @Test(expected = IllegalStateException::class)
    fun `a blocked evidence directory fails explicitly`() {
        File(temporaryFolder.root, "quest-proofs").writeText("not a directory")
        JpegPhotoProofFileFactory(temporaryFolder.root).create("botanical-garden", 0)
    }
}
