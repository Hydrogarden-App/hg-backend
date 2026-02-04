package com.hydrogarden.business.device.core.commands;


public interface DeviceCommandVisitor {
    void visit(KeepAliveCommand cmd, DeviceContext now);

    void visit(HeartbeatCommand cmd, DeviceContext now);

    void visit(NewStateCommand cmd, DeviceContext now);

    void visit(AckStateCommand cmd, DeviceContext now);

    void visit(ConfigCommand configCommand, DeviceContext deviceContext);

    void visit(AckConfigCommand ackConfigCommand, DeviceContext deviceContext);

    void visit(RequestConfigCommand requestConfigCommand, DeviceContext deviceContext);
}
