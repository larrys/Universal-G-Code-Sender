/*
    Copyright 2015-2022 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.ugs.nbp.core.actions;

import com.willwinder.ugs.nbp.lib.lookup.CentralLookup;
import com.willwinder.ugs.nbp.lib.services.LocalizingService;
import com.willwinder.universalgcodesender.listeners.UGSEventListener;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.UGSEvent;
import com.willwinder.universalgcodesender.model.events.ControllerStateEvent;
import com.willwinder.universalgcodesender.model.events.FileStateEvent;
import com.willwinder.universalgcodesender.utils.GUIHelpers;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

@ActionID(
        category = LocalizingService.StartCategory,
        id = LocalizingService.StartActionId)
@ActionRegistration(
        iconBase = StartAction.ICON_BASE,
        displayName = "resources/MessagesBundle#" + LocalizingService.StartTitleKey,
        lazy = false)
@ActionReferences({
        @ActionReference(
                path = LocalizingService.StartWindowPath,
                position = 1000),
        @ActionReference(
                path = "Toolbars/StartPauseStop",
                position = 1000)
})
public final class StartAction extends ProgramAction implements UGSEventListener {

    public static final String ICON_BASE = "resources/icons/start.svg";

    private final transient BackendAPI backend;

    public StartAction() {
        this(CentralLookup.getDefault().lookup(BackendAPI.class));
    }

    public StartAction(BackendAPI backendAPI) {
        this.backend = backendAPI;
        this.backend.addUGSEventListener(this);

        putValue("iconBase", ICON_BASE);
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(ICON_BASE, false));
        putValue("menuText", LocalizingService.StartTitle);
        putValue(NAME, LocalizingService.StartTitle);
        setEnabled(isEnabled());
    }

    @Override
    public void UGSEvent(UGSEvent cse) {
        if (cse instanceof ControllerStateEvent || cse instanceof FileStateEvent) {
            EventQueue.invokeLater(() -> setEnabled(isEnabled()));
        }
    }

    @Override
    public boolean isEnabled() {
        return backend.canSend() || backend.isPaused();
    }

    @Override
    public void execute(ActionEvent e) {
        try {
            if (backend.isPaused()) {
                backend.pauseResume();
            } else {
                backend.send();
            }
        } catch (Exception ex) {
            GUIHelpers.displayErrorDialog(ex.getLocalizedMessage());
        }
    }
}
