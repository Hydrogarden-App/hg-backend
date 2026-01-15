package com.hydrogarden.business.device.core.dto;

import com.hydrogarden.business.device.core.commands.*;
import com.hydrogarden.business.device.core.entity.CircuitState;
import com.hydrogarden.business.device.core.entity.DeviceId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

public class DeviceCommandTest {



    @Test
    public void testKeepAliveCommand(){

        DeviceCommand deviceCommand = new KeepAliveCommand(new DeviceId((short) 1));
        byte[] byteArray = deviceCommand.toBytes();
        String binaryString = bytearrToString(byteArray);
        Assertions.assertEquals("00000000000000010000000100000000", binaryString);
    }

    @Test
    public void testHeartbeatCommand(){

        DeviceCommand deviceCommand = new HeartbeatCommand(new DeviceId((short) 1));
        byte[] byteArray = deviceCommand.toBytes();
        String binaryString = bytearrToString(byteArray);
        Assertions.assertEquals("00000000000000010000001100000000", binaryString);
    }


    static Stream<Arguments> stringProvider() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(List.of(true, false, true, false, true, false, true), "0000000000000001000001000000000110101010"),
                org.junit.jupiter.params.provider.Arguments.of(List.of(true, false, true, false, true, false, true, false), "0000000000000001000001000000000110101010"),
                org.junit.jupiter.params.provider.Arguments.of(List.of(true, false, true, false, true, false, true, false, true), "000000000000000100000100000000101010101010000000"),
                org.junit.jupiter.params.provider.Arguments.of(List.of(true, false, true, false, true, false, true, false, true, false, true, false, true, false, true), "000000000000000100000100000000101010101010101010"),
                org.junit.jupiter.params.provider.Arguments.of(List.of(true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false), "000000000000000100000100000000101010101010101010"),
                org.junit.jupiter.params.provider.Arguments.of(List.of(true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true), "00000000000000010000010000000011101010101010101010000000")



        );
    }

    @ParameterizedTest
    @MethodSource("stringProvider")
    public void testNewStateCommand(List<Boolean> booleans, String result){
        List<CircuitState> circuitStates = booleans.stream().map(b -> new CircuitState()).toList();
        DeviceCommand deviceCommand = new NewStateCommand(new DeviceId((short) 1), circuitStates);
        byte[] byteArray = deviceCommand.toBytes();
        String binaryString = bytearrToString(byteArray);
        Assertions.assertEquals(result, binaryString);
    }



    private static String bytearrToString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        String binaryString = sb.toString();
        return binaryString;
    }
}
