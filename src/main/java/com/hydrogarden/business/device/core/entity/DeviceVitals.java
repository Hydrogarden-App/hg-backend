package com.hydrogarden.business.device.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = false)
@Entity
@Setter
@Table(name = "device_vitals")
@NoArgsConstructor
@AllArgsConstructor
public class DeviceVitals {

    @Getter
    @EmbeddedId
    @AttributeOverrides({@AttributeOverride(name = "id", column = @Column(name = "id"))})
    private DeviceId id;

    /**
     * Current known operational state of the device.
     */
    @Getter
    @Enumerated(EnumType.STRING)
    private DeviceState state;

    /**
     * Desired state of the device; indicates what state the system wants the device to be in
     */
    @Getter
    @Enumerated(EnumType.STRING)
    private DeviceState desiredState;

    /**
     * Timestamp when the last KeepAlive command was sent
     */
    @Getter
    @Column
    private LocalDateTime lastKeepAliveSendTime;

    /**
     * Timestamp of the last heartbeat signal received from the device
     */
    @Getter
    @Column
    private LocalDateTime lastCommandReceiveTime;

    @Column
    @Getter
    @Setter
    @AttributeOverrides({@AttributeOverride(name = "standbyTimeout", column = @Column(name = "standbyTimeout")), @AttributeOverride(name = "heartbeatInterval", column = @Column(name = "heartbeatInterval"))

    })
    private DeviceConfig deviceConfig;

    @Column
    @Getter
    @Setter
    @AttributeOverrides({@AttributeOverride(name = "standbyTimeout", column = @Column(name = "desiredStandbyTimeout")), @AttributeOverride(name = "heartbeatInterval", column = @Column(name = "desiredHeartbeatInterval"))})
    private DeviceConfig desiredDeviceConfig;

    @Getter
    @Column
    private LocalDateTime lastConfigSendTime;

    @Getter
    @Column
    private LocalDateTime lastNewStateSendTime;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Device device;
}
