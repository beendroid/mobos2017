/*
 * Copyright 2017 Ciorceri Petru Sorin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.hx711;

import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

/**
 * Device driver for HX711 (24-Bit Analog-to-Digital Converter [ADC] for Weight Scales).
 *
 * For information on the HX711, see:
 *   https://cdn.sparkfun.com/datasheets/Sensors/ForceFlex/hx711_english.pdf
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class hx711 implements AutoCloseable {
    private static final String TAG = "hx711";
    private int offset = 0;
    private double scale = 1.0;

    private static final int SPI_FREQUENCY = 115200;
    private static final int SPI_MODE = SpiDevice.MODE0;
    private static final int SPI_BPW = 8;   // bits per word

    /**
     * The gain of HX711
     */
    public enum Gain {
        Gain32(0), Gain64(1), Gain128(2);
        int value;

        Gain(int value) {
            this.value = value;
        }
    }

    /**
     * The clock pulses used to receive weight and set gain
     */
    public byte[][] gainArray = {
            {(byte) 0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xA0},
            {(byte) 0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xA8},
            {(byte) 0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0x80}
    };

    private Gain mGain;
    private String mSpiBusPort;
    private SpiDevice spiDevice = null;

    /**
     * Create a new hx711 driver.
     *
     * @param spiBusPort Name of the SPI bus
     * @param gain (32, 64, 128)
     */
    public hx711(String spiBusPort, Gain gain) throws IOException {
        setGain(gain);
        setSpiBusPort(spiBusPort);
    }

    public void setGain(Gain gain) { mGain = gain; }

    public void setOffset(int offset) { this.offset = offset; }

    public void setScale(double scale) { this.scale = scale; }

    public void setSpiBusPort(String spiPort) { mSpiBusPort = spiPort; }

    public int getOffset() {
        return this.offset;
    }

    /**
     * Initial configuration of driver
     * @param device
     * @throws IOException
     */
    private void configure(SpiDevice device) throws IOException {
        // Note: You may need to set bit justification for your board.
        // spiDevice.setBitJustification(SPI_BITJUST);
        device.setFrequency(SPI_FREQUENCY);
        device.setMode(SPI_MODE);
        device.setBitsPerWord(SPI_BPW);
    }

    /**
     * Returns true is HX711 is ready to read weight
     * @return
     */
    public boolean isReady() throws IOException {
        byte[] txBuffer= { (byte) 0x00 };
        byte[] response = new byte[1];
        readRaw(txBuffer, response);
        for (byte i=0; i<response.length; i++) {
            if (response[i] != 0x00) {
                return false;
            }
        }
        return true;
    }

    /**
     * Do a calibration with a known weight (units)
     * @param units
     * @param times
     * @throws IOException
     */
    public void calibrateUnits(int units, int times) throws IOException {
        int curentValue = readAverage(times);
        setScale(curentValue / units);
        Log.d(TAG + " new scale", "" + this.scale);
    }

    /**
     * Get a reading in KG (or other units)
     * @param times
     * @return
     * @throws IOException
     */
    public double getUnits(int times) throws IOException {
        return (readAverage(times) / this.scale);
    }

    /**
     * It reads the HX711 ADC value (multiple reads)
     * @param times
     * @return
     */
    public int readAverage(int times) throws IOException {
        long sum = 0;
        for (int i=0; i<times; i++) {
            sum += read();
        }
        return (int) (sum / times) - this.offset;
    }

    /**
     * It reads the HX711 ADC value (one read)
     * @return
     */
    private int read() throws IOException {
        int value = 0;
        byte[] txBuffer = gainArray[mGain.value];
        byte[] response = new byte[10];
        byte[] response_complement = new byte[10];

        while (!isReady()) {}

        readRaw(txBuffer, response);

        // simple response validation
        boolean validResponse = false;
        for (byte i=0; i<=6; i++) {
            if (response[i] != 0)   // first 7 bytes should be not 0
                validResponse = true;
        }
        for (byte i=7; i<=9; i++) {
            if (response[i] != 0)   // last 3 bytes must be 0
                validResponse = false;
        }
        if (! validResponse)
            throw new IOException("Received an invalid response from HX711");

        // convert response to useful value
        for (byte i=0; i<6; i++) {
            response[i] = (byte) ~response[i];
            response_complement[i] = (byte) (((response[i] & 0b01000000) >> 3) +
                    ((response[i] & 0b00010000) >> 2) +
                    ((response[i] & 0b00000100) >> 1) +
                    ((response[i] & 0b00000001) >> 0));
        }
        value = (response_complement[0] << 20) +
                (response_complement[1] << 16) +
                (response_complement[2] << 12) +
                (response_complement[3] << 8) +
                (response_complement[4] << 4) +
                response_complement[5];

        return value;
    }

    /**
     * The RAW way to do a read (txBuffer should be the Clock signal)
     * @param txBuffer
     * @param response
     * @throws IOException
     */
    private void readRaw(byte[] txBuffer, byte[] response) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();

        spiDevice = pioService.openSpiDevice(mSpiBusPort);
        try {
            configure(spiDevice);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }

        spiDevice.transfer(txBuffer, response, txBuffer.length);

        if (spiDevice != null) {
            spiDevice.close();
            spiDevice = null;
        }
    }

    /**
     * It sets the offset value for tare weight
     * @param times
     */
    public void tare(int times) throws IOException {
        int offset = readAverage(times);
        setOffset(offset);
        Log.d(TAG + " new offset", "" + offset);
    }

    /**
     * Releases the SPI interface and related resources.
     */
    @Override
    public void close() throws IOException {
        if (spiDevice != null) {
            try {
                spiDevice.close();
            } finally {
                spiDevice = null;
            }
        }
    }
}
