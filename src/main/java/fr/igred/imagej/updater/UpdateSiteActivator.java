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
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public final class UpdateSiteActivator {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private static final CommandLine cmd = new CommandLine(new File(IJ.getDir("imagej")), 100);


    private UpdateSiteActivator() {
    }


    private static GenericDialog createConfirmationDialog(String updateSite) {
        String        title   = "Activate update site?";
        String        message = String.format("An update site (%s) should be activated. Do you accept it?", updateSite);
        GenericDialog dialog  = new GenericDialog(title);
        dialog.addMessage(message);
        return dialog;
    }


    static UpdateSite findUpdateSite(String name) throws IOException {
        return AvailableSites.getAvailableSites()
                             .values()
                             .stream()
                             .collect(Collectors.toMap(UpdateSite::getName,
                                                       Function.identity()))
                             .get(name);
    }


    public static void update() {
        new CheckForUpdates().run();
        cmd.refreshUpdateSites(new ArrayList<>(0));
        cmd.update(new ArrayList<>(0));
    }


    public static boolean activate(String name, String url) throws IOException {
        boolean activated = false;
        boolean inactive  = true;
        boolean urlMatch  = true;

        List<String> args = new ArrayList<>(2);

        UpdateSite site = findUpdateSite(name);
        if (site != null) {
            String siteURL = site.getURL();
            args.add(site.getName());
            args.add(siteURL);
            if (site.isActive()) inactive = false;
            if (url.equals(siteURL)) urlMatch = false;
            if (inactive || urlMatch) {
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
        if (!urlMatch) {
            String msg = String.format("Provided URL does not match known URL for \"%s\".", name);
            logger.warning(msg);
        }
        return activated;
    }


    public static boolean activate(String name) throws IOException {
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


    public static boolean activateAndUpdate(String name) throws IOException {
        boolean activated = activate(name);
        if (activated) {
            update();
            IJ.log("Update completed. Please restart Fiji.");
        }
        return activated;
    }


    public static boolean activateAndUpdate(String name, String url) throws IOException {
        boolean activated = activate(name, url);
        if (activated) {
            update();
            IJ.log("Update completed. Please restart Fiji.");
        }
        return activated;
    }


    public static void confirmActivation(String name) throws IOException {
        GenericDialog dialog = createConfirmationDialog(name);
        dialog.showDialog();
        if (dialog.wasOKed())
            activateAndUpdate(name);
    }


    public static void confirmActivation(String name, String url) throws IOException {
        GenericDialog dialog = createConfirmationDialog(String.format("%s - %s", name, url));
        dialog.showDialog();
        if (dialog.wasOKed())
            activateAndUpdate(name, url);
    }

}
