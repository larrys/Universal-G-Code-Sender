/*
    Copyright 2023 Will Winder

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
package com.willwinder.universalgcodesender.connection;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * A connection device based on a JSerialComm serial port.
 *
 * @author Joacim Breiler
 */
public class JSerialCommConnectionDevice extends AbstractConnectionDevice {
    private final SerialPort serialPort;

    public JSerialCommConnectionDevice(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    @Override
    public String getAddress() {
        return serialPort.getSystemPortName();
    }

    @Override
    public Optional<String> getDescription() {
        String description = serialPort.getPortDescription();
        if (StringUtils.isEmpty(description)) {
            return Optional.empty();
        }
        return Optional.of(description);
    }

    @Override
    public Optional<Integer> getPort() {
        return Optional.empty();
    }

    @Override
    public  Optional<String> getManufacturer() {
        String manufacturer = serialPort.getManufacturer();
        if (StringUtils.isEmpty(manufacturer)) {
            return Optional.empty();
        }
        return Optional.of(manufacturer);
    }
}
