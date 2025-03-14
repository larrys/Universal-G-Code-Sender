/*
    Copyright 2016-2024 Will Winder

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
package com.willwinder.ugs.nbm.visualizer.renderables;

import static com.jogamp.opengl.GL.GL_LINES;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions;
import static com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions.VISUALIZER_OPTION_BOUNDARY_INVERT_X;
import static com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions.VISUALIZER_OPTION_BOUNDARY_INVERT_Y;
import static com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions.VISUALIZER_OPTION_BOUNDARY_INVERT_Z;
import static com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions.VISUALIZER_OPTION_BOUNDRY;
import static com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions.VISUALIZER_OPTION_X;
import static com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions.VISUALIZER_OPTION_Y;
import static com.willwinder.ugs.nbm.visualizer.options.VisualizerOptions.VISUALIZER_OPTION_Z;
import com.willwinder.ugs.nbm.visualizer.shared.Renderable;
import com.willwinder.ugs.nbp.lib.lookup.CentralLookup;
import com.willwinder.universalgcodesender.CapabilitiesConstants;
import com.willwinder.universalgcodesender.firmware.FirmwareSettingsException;
import com.willwinder.universalgcodesender.firmware.IFirmwareSettings;
import com.willwinder.universalgcodesender.listeners.ControllerState;
import com.willwinder.universalgcodesender.model.Axis;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.PartialPosition;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UGSEvent;
import com.willwinder.universalgcodesender.model.UnitUtils;
import com.willwinder.universalgcodesender.model.events.ControllerStateEvent;
import com.willwinder.universalgcodesender.model.events.FirmwareSettingEvent;

/**
 * Displays the machine boundries based on the soft limits
 *
 * @author Joacim Breiler
 */
public class MachineBoundries extends Renderable {
    private final BackendAPI backendAPI;
    private PartialPosition minPosition = new PartialPosition(0d, 0d, 0d, UnitUtils.Units.MM);
    private float[] machineBoundryBottomColor;
    private float[] machineBoundryLineColor;
    private float[] yAxisColor;
    private float[] xAxisColor;
    private float[] zAxisColor;
    private boolean softLimitsEnabled = false;
    private int invertX = 1;
    private int invertY = 1;
    private int invertZ = 1;

    /**
     * Construct with a priority number. Objects should be rendered from highest
     * to lowest priority.
     */
    public MachineBoundries(String title) {
        super(Integer.MIN_VALUE, title, VISUALIZER_OPTION_BOUNDRY);
        reloadPreferences(new VisualizerOptions());
        backendAPI = CentralLookup.getDefault().lookup(BackendAPI.class);
        backendAPI.addUGSEventListener(this::onUGSEvent);
    }

    private void onUGSEvent(UGSEvent event) {
        if (!isFirmwareSettingsEvent(event) && !isControllerIdleEvent(event)) {
            return;
        }

        try {
            IFirmwareSettings firmwareSettings = backendAPI.getController().getFirmwareSettings();
            softLimitsEnabled = firmwareSettings.isSoftLimitsEnabled();
            if (!softLimitsEnabled) {
                return;
            }

            PartialPosition.Builder minPositionBuilder = PartialPosition.builder(UnitUtils.Units.MM);
            for (Axis axis : Axis.values()) {
                double softLimit = firmwareSettings.getSoftLimit(axis);
                minPositionBuilder.setValue(axis, -softLimit);
            }
            minPosition = minPositionBuilder.build();
            updateSettingsFromController();
        } catch (FirmwareSettingsException ignored) {
            // Never mind this.
        }
    }

    private boolean isControllerIdleEvent(UGSEvent event) {
        return event instanceof ControllerStateEvent controllerStateEvent && (controllerStateEvent.getState() == ControllerState.IDLE || controllerStateEvent.getState() == ControllerState.ALARM);
    }

    private boolean isFirmwareSettingsEvent(UGSEvent event) {
        // This will prevent us from accessing the firmware settings before the init
        // processes has finished and it will also prevent us from accessing the
        // controller after it has disconnected
        return backendAPI.isConnected() && event instanceof FirmwareSettingEvent;
    }

    @Override
    public boolean rotate() {
        return true;
    }

    @Override
    public boolean center() {
        return true;
    }

    @Override
    public boolean enableLighting() {
        return false;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // Not used
    }

    @Override
    public void reloadPreferences(VisualizerOptions vo) {
        super.reloadPreferences(vo);
        machineBoundryBottomColor = VisualizerOptions.colorToFloatArray(vo.getOptionForKey(VisualizerOptions.VISUALIZER_OPTION_BOUNDRY_BASE).value);
        machineBoundryLineColor = VisualizerOptions.colorToFloatArray(vo.getOptionForKey(VisualizerOptions.VISUALIZER_OPTION_BOUNDRY_SIDES).value);
        xAxisColor = VisualizerOptions.colorToFloatArray(vo.getOptionForKey(VISUALIZER_OPTION_X).value);
        yAxisColor = VisualizerOptions.colorToFloatArray(vo.getOptionForKey(VISUALIZER_OPTION_Y).value);
        zAxisColor = VisualizerOptions.colorToFloatArray(vo.getOptionForKey(VISUALIZER_OPTION_Z).value);
        updateSettingsFromController();
    }

    private void updateSettingsFromController() {
        BackendAPI backendAPI = CentralLookup.getDefault().lookup(BackendAPI.class);
        invertX = VisualizerOptions.getBooleanOption(VISUALIZER_OPTION_BOUNDARY_INVERT_X, false) ? -1 : 1;
        invertY = VisualizerOptions.getBooleanOption(VISUALIZER_OPTION_BOUNDARY_INVERT_Y, false) ? -1 : 1;
        invertZ = VisualizerOptions.getBooleanOption(VISUALIZER_OPTION_BOUNDARY_INVERT_Z, false) ? -1 : 1;

        if (backendAPI == null || backendAPI.getController() == null) {
            return;
        }

        boolean homingSetsZeroPosition = backendAPI.getController().getCapabilities().hasCapability(CapabilitiesConstants.HOMING_SETS_MACHINE_ZERO_POSITION);
        if (homingSetsZeroPosition) {
            invertBasedOnHomingDirection(backendAPI);
        }
    }

    private void invertBasedOnHomingDirection(BackendAPI backendAPI) {
        IFirmwareSettings settings = backendAPI.getController().getFirmwareSettings();
        invertX = isHomingDirectionInverted(settings, Axis.X) ? -invertX : invertX;
        invertY = isHomingDirectionInverted(settings, Axis.Y) ? -invertY : invertY;
        invertZ = isHomingDirectionInverted(settings, Axis.Z) ? -invertZ : invertZ;
    }

    private static boolean isHomingDirectionInverted(IFirmwareSettings settings, Axis axis) {
        return settings.isHomingDirectionInverted(axis);
    }

    @Override
    public void draw(GLAutoDrawable drawable, boolean idle, Position machineCoord, Position workCoord, Position focusMin, Position focusMax, double scaleFactor, Position mouseWorldCoordinates, Position rotation) {
        if (!softLimitsEnabled) {
            return;
        }
        double xOffset = workCoord.x - machineCoord.x;
        double yOffset = workCoord.y - machineCoord.y;
        double zOffset = workCoord.z - machineCoord.z;

        Position bottomLeft = new Position((minPosition.getX() * invertX) + xOffset, (minPosition.getY() * invertY) + yOffset, (minPosition.getZ() * invertZ) + zOffset);
        Position topRight = new Position(xOffset, yOffset, zOffset);

        GL2 gl = drawable.getGL().getGL2();
        gl.glPushMatrix();
        drawBase(gl, bottomLeft, topRight);
        drawSides(gl, bottomLeft, topRight);
        drawAxisLines(gl, bottomLeft, topRight);
        gl.glPopMatrix();
    }

    private void drawBase(GL2 gl, Position bottomLeft, Position topRight) {
        double bottomZ = Math.min(bottomLeft.getZ(), topRight.getZ());
        gl.glColor4fv(machineBoundryBottomColor, 0);
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex3d(bottomLeft.x, bottomLeft.y, bottomZ);
        gl.glVertex3d(bottomLeft.x, topRight.y, bottomZ);
        gl.glVertex3d(topRight.x, topRight.y, bottomZ);
        gl.glVertex3d(topRight.x, bottomLeft.y, bottomZ);
        gl.glEnd();
    }

    private void drawAxisLines(GL2 gl, Position bottomLeft, Position topRight) {
        double offset = 0.001;
        gl.glLineWidth(5f);
        gl.glBegin(GL_LINES);
        // X Axis Line
        gl.glColor4fv(yAxisColor, 0);
        gl.glVertex3d(0, bottomLeft.y, offset);
        gl.glVertex3d(0, topRight.y, offset);

        gl.glVertex3d(0, bottomLeft.y, offset);
        gl.glVertex3d(0, topRight.y, offset);

        // Y Axis Line
        gl.glColor4fv(xAxisColor, 0);
        gl.glVertex3d(bottomLeft.x, 0, offset);
        gl.glVertex3d(topRight.x, 0, offset);

        gl.glVertex3d(bottomLeft.x, 0, offset);
        gl.glVertex3d(topRight.x, 0, offset);

        // Z Axis Line
        gl.glColor4fv(zAxisColor, 0);
        gl.glVertex3d(0, 0, bottomLeft.z);
        gl.glVertex3d(0, 0, Math.max(topRight.z, -bottomLeft.z));
        gl.glEnd();
    }

    private void drawSides(GL2 gl, Position bottomLeft, Position topRight) {
        double offset = 0.001;
        gl.glLineWidth(3f);
        gl.glBegin(GL_LINES);
        gl.glColor4fv(machineBoundryLineColor, 0);
        gl.glVertex3d(bottomLeft.x, bottomLeft.y, bottomLeft.getZ() + offset);
        gl.glVertex3d(bottomLeft.x, topRight.y, bottomLeft.getZ() + offset);
        gl.glVertex3d(bottomLeft.x, topRight.y, bottomLeft.getZ() + offset);
        gl.glVertex3d(topRight.x, topRight.y, bottomLeft.getZ() + offset);
        gl.glVertex3d(topRight.x, topRight.y, bottomLeft.getZ() + offset);
        gl.glVertex3d(topRight.x, bottomLeft.y, bottomLeft.getZ() + offset);
        gl.glVertex3d(topRight.x, bottomLeft.y, bottomLeft.getZ() + offset);
        gl.glVertex3d(bottomLeft.x, bottomLeft.y, bottomLeft.getZ() + offset);

        gl.glVertex3d(bottomLeft.x, bottomLeft.y, topRight.getZ() + offset);
        gl.glVertex3d(bottomLeft.x, topRight.y, topRight.getZ() + offset);
        gl.glVertex3d(bottomLeft.x, topRight.y, topRight.getZ() + offset);
        gl.glVertex3d(topRight.x, topRight.y, topRight.getZ() + offset);
        gl.glVertex3d(topRight.x, topRight.y, topRight.getZ() + offset);
        gl.glVertex3d(topRight.x, bottomLeft.y, topRight.getZ() + offset);
        gl.glVertex3d(topRight.x, bottomLeft.y, topRight.getZ() + offset);
        gl.glVertex3d(bottomLeft.x, bottomLeft.y, topRight.getZ() + offset);

        gl.glVertex3d(bottomLeft.x, bottomLeft.y, bottomLeft.getZ());
        gl.glVertex3d(bottomLeft.x, bottomLeft.y, topRight.getZ());

        gl.glVertex3d(bottomLeft.x, topRight.y, bottomLeft.getZ());
        gl.glVertex3d(bottomLeft.x, topRight.y, topRight.getZ());

        gl.glVertex3d(topRight.x, bottomLeft.y, bottomLeft.getZ());
        gl.glVertex3d(topRight.x, bottomLeft.y, topRight.getZ());

        gl.glVertex3d(topRight.x, topRight.y, bottomLeft.getZ());
        gl.glVertex3d(topRight.x, topRight.y, topRight.getZ());
        gl.glEnd();
    }
}
