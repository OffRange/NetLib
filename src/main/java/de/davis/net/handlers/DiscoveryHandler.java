package de.davis.net.handlers;

import de.davis.net.sync.models.UdpModel;

import java.net.DatagramPacket;

public interface DiscoveryHandler<M extends UdpModel> {

    void onServerDiscovered(M model, DatagramPacket packet);
    void onFinish();
}
