/*
 * Copyright (C) 2020 - 2023 GReD
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package fr.igred.imagej.updater;


import net.imagej.updater.UpdateSite;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


class UpdateSiteActivatorTest {


    @SuppressWarnings("AccessOfSystemProperties")
    private static final String              IJ_PATH = System.getProperty("user.dir") + File.separator + "IJ";
    private final        UpdateSiteActivator usa     = new UpdateSiteActivator();


    private static void deleteRecursively(Path rootPath) throws IOException {
        try (Stream<Path> walk = Files.walk(rootPath)) {
            //noinspection ResultOfMethodCallIgnored
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }


    @BeforeAll
    static void setUp() throws IOException {
        cleanUp();

        String  pluginsPath = IJ_PATH + File.separator + "plugins";
        File    pluginsDir  = new File(pluginsPath);
        boolean ignored     = pluginsDir.mkdirs();

        File ijDir = new File(IJ_PATH);
        assumeTrue(ijDir.exists(), "IJ directory does not exist.");

        //noinspection AccessOfSystemProperties
        System.setProperty("plugins.dir", pluginsPath);
    }


    @AfterAll
    static void cleanUp() throws IOException {
        File ijDir = new File(IJ_PATH);
        if (ijDir.exists()) deleteRecursively(ijDir.toPath());
    }


    @Test
    void testFindUpdateSite() {
        UpdateSite site = usa.findUpdateSite("Fiji");
        assertEquals("https://update.fiji.sc/", site.getURL(), "URL mismatch.");
    }


    @Test
    void testFindUpdateSiteError() {
        UpdateSite site = usa.findUpdateSite("Tutu");
        assertNull(site, "Site should not have been found.");
    }


    @ParameterizedTest
    @CsvSource({"OMERO 5.5-5.6,true", "OMERO 5.5-5.6,false", "tutu,false"})
    void testActivate1(String name, boolean result) {
        boolean activated = usa.activate(name);
        assertEquals(result, activated, "Site activation did not go as expected.");
    }


    @SuppressWarnings("HardcodedFileSeparator")
    @ParameterizedTest
    @CsvSource({"tata,https://localhost/tata/,true",
                "tata,https://localhost/tutu/,true",
                "tata,https://localhost/tutu/,false"})
    void testActivate2(String name, String url, boolean result) {
        boolean activated = usa.activate(name, url);
        assertEquals(result, activated, "Site activation did not go as expected.");
    }


    @Test
    void testActivateAndUpdate1() {
        boolean activated = usa.activateAndUpdate("IJPB-plugins");
        assertTrue(activated, "IJPB-plugins was not activated.");

        File   pluginsDir = new File(IJ_PATH + File.separator + "plugins");
        File[] files      = pluginsDir.listFiles(new NameFilter("MorphoLibJ_-.*.jar"));
        assertNotNull(files, "Matching files is a null array.");
        assertNotEquals(0, files.length, "No matching file found.");
    }


    @Test
    void testActivateAndUpdate2() {
        boolean activated = usa.activateAndUpdate("ilastik", "https://sites.imagej.net/Ilastik/");
        assertTrue(activated, "ilastik was not activated.");

        File   pluginsDir = new File(IJ_PATH + File.separator + "plugins");
        File[] files      = pluginsDir.listFiles(new NameFilter("ilastik4ij-.*.jar"));
        assertNotNull(files, "Matching files is a null array.");
        assertNotEquals(0, files.length, "No matching file found.");
    }


    private static class NameFilter implements FilenameFilter {

        private final String regex;


        NameFilter(String regex) {
            this.regex = regex;
        }


        @Override
        public boolean accept(File dir, String name) {
            return name.matches(regex);
        }

    }

}
