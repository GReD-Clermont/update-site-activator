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
/*
 *  Copyright (C) 2020-2022 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.

 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package fr.igred.imagej.updater;


import ij.IJ;
import ij.gui.GenericDialog;
import net.imagej.updater.CheckForUpdates;
import net.imagej.updater.CommandLine;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;
import net.imagej.updater.util.StderrProgress;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * Class to activate update sites and download their files.
 */
public class UpdateSiteActivator {

    /** The logger for this class. */
    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    /** The command line interface. */
    private static final CommandLine cmd = new CommandLine(new File(IJ.getDir("imagej")), 100);

    /** ImageJ directory. */
    private final File            ijDir = new File(IJ.getDir("imagej"));
    /** Collection of file objects. */
    private final FilesCollection files = new FilesCollection(ijDir);


    /**
     * Default constructor. Initializes update sites, downloads index and checksums.
     */
    public UpdateSiteActivator() {
        AvailableSites.initializeAndAddSites(files);
        String warnings = "";
        try {
            warnings = files.downloadIndexAndChecksum(new StderrProgress(100));
        } catch (ParserConfigurationException | SAXException e) {
            logger.warning(e.getMessage());
        }
        if (!warnings.isEmpty()) {
            logger.warning(warnings);
        }
    }


    /** Creates a confirmation dialog. */
    private static GenericDialog createConfirmationDialog(String updateSite) {
        String        title   = "Activate update site?";
        String        message = String.format("An update site (%s) should be activated. Do you accept it?", updateSite);
        GenericDialog dialog  = new GenericDialog(title);
        dialog.addMessage(message);
        return dialog;
    }


    /** Updates Fiji. */
    public static void update() {
        new CheckForUpdates().run();
        cmd.refreshUpdateSites(new ArrayList<>(0));
        cmd.update(new ArrayList<>(0));
    }


    /**
     * Finds an UpdateSite with the given name.
     *
     * @param name The name of the UpdateSite.
     *
     * @return The UpdateSite.
     */
    UpdateSite findUpdateSite(String name) {
        return files.getUpdateSite(name, true);
    }


    /**
     * Activates an update site with the given name and URL.
     *
     * @param name The name of the update site.
     * @param url  The URL of the update site.
     *
     * @return {@code true} if the site was activated, {@code false} otherwise.
     */
    public boolean activate(String name, String url) {
        boolean activated   = false;
        boolean inactive    = true;
        boolean urlMismatch = false;

        List<String> args = new ArrayList<>(2);

        UpdateSite site = findUpdateSite(name);
        if (site != null) {
            String siteURL = site.getURL();
            args.add(site.getName());
            args.add(url);
            if (site.isActive()) inactive = false;
            if (!url.equals(siteURL)) urlMismatch = true;
            if (inactive || urlMismatch) {
                cmd.addOrEditUploadSite(args, false);
                activated = true;
            }
        } else {
            args.add(name);
            args.add(url);
            cmd.addOrEditUploadSite(args, true);
            activated = true;
        }
        if (!inactive) {
            String msg = String.format("Update site \"%s\" is already active.", name);
            logger.warning(msg);
        }
        if (urlMismatch) {
            String msg = String.format("Provided URL does not match known URL for \"%s\".", name);
            logger.warning(msg);
        }
        return activated;
    }


    /**
     * Activates an update site with the given name.
     *
     * @param name The name of the update site.
     *
     * @return {@code true} if the site was activated, {@code false} otherwise.
     */
    public boolean activate(String name) {
        boolean activated = false;

        UpdateSite site = findUpdateSite(name);
        if (site == null) {
            String msg = String.format("Update site \"%s\" could not be found.", name);
            logger.info(msg);
        } else if (!site.isActive()) {
            List<String> args = new ArrayList<>(2);
            args.add(site.getName());
            args.add(site.getURL());
            cmd.addOrEditUploadSite(args, false);
            activated = true;
        } else {
            String msg = String.format("Update site \"%s\" is already active.", name);
            logger.warning(msg);
        }

        return activated;
    }


    /**
     * Activates an update site with the given name and starts the update.
     *
     * @param name The name of the update site.
     *
     * @return {@code true} if the site was activated, {@code false} otherwise.
     */
    public boolean activateAndUpdate(String name) {
        boolean activated = activate(name);
        if (activated) {
            update();
            IJ.log("Update completed. Please restart Fiji.");
        }
        return activated;
    }


    /**
     * Activates an update site with the given name and URL, and starts the update.
     *
     * @param name The name of the update site.
     * @param url  The URL of the update site.
     *
     * @return {@code true} if the site was activated, {@code false} otherwise.
     */
    public boolean activateAndUpdate(String name, String url) {
        boolean activated = activate(name, url);
        if (activated) {
            update();
            IJ.log("Update completed. Please restart Fiji.");
        }
        return activated;
    }


    /**
     * Asks the user for a confirmation before activating the update site with the given name, and updating Fiji.
     *
     * @param name The name of the update site.
     */
    public void confirmActivation(String name) {
        GenericDialog dialog = createConfirmationDialog(name);
        dialog.showDialog();
        if (dialog.wasOKed())
            activateAndUpdate(name);
    }


    /**
     * Asks the user for a confirmation before activating the update site with the given name and URL, and updating
     * Fiji.
     *
     * @param name The name of the update site.
     * @param url  The URL of the update site.
     */
    public void confirmActivation(String name, String url) {
        GenericDialog dialog = createConfirmationDialog(String.format("%s - %s", name, url));
        dialog.showDialog();
        if (dialog.wasOKed())
            activateAndUpdate(name, url);
    }


    @Override
    public String toString() {
        return "UpdateSiteActivator{" +
               "ijDir=" + ijDir +
               "}";
    }

}
