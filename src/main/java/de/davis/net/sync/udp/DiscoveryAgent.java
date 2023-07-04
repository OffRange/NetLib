package de.davis.net.sync.udp;

import com.google.gson.Gson;
import de.davis.net.handlers.DiscoveryHandler;
import de.davis.net.handlers.ErrorHandler;
import de.davis.net.sync.AbstractBuilder;
import de.davis.net.sync.Client;
import de.davis.net.sync.models.UdpModel;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;

public class DiscoveryAgent<M extends UdpModel> extends Thread implements Client {

    private final Class<M> modelClass;

    private final int port;

    private final Gson gson;

    private final int timeout;

    private final DiscoveryHandler<M> discoveryHandler;
    private final ErrorHandler errorHandler;

    public DiscoveryAgent(Class<M> modelClass, int port, int timeout, DiscoveryHandler<M> discoveryHandler, ErrorHandler errorHandler, Gson gson) {
        this.modelClass = modelClass;
        this.port = port;
        this.timeout = timeout;

        this.discoveryHandler = discoveryHandler;
        this.errorHandler = errorHandler;

        this.gson = gson;
    }

    @Override
    public void run() {
        try(DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(timeout);

            M model = modelClass.getDeclaredConstructor(Client.class).newInstance(this);

            byte[] data = gson.toJson(model).getBytes();
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(getSubnetBroadcastAddress()), port));

            byte[] receiveData = new byte[2048];

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (true) {
                try {
                    socket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    model = gson.fromJson(response, modelClass);

                    if(discoveryHandler == null)
                        continue;

                    discoveryHandler.onServerDiscovered(model, receivePacket);

                } catch (SocketTimeoutException e) {
                    if(discoveryHandler != null)
                        discoveryHandler.onFinish();
                    break;
                }
            }
        } catch (IOException e) {
            if(errorHandler != null)
                errorHandler.onErrorOccurred(this, e, ErrorHandler.Type.UDP_DISCOVERING);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getSubnetBroadcastAddress() {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                InetAddress subnet = address.getBroadcast();
                if (subnet != null) {
                    return subnet.getHostAddress();
                }
            }
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class Builder<M extends UdpModel> extends AbstractBuilder<Builder<M>, DiscoveryAgent<M>> {

        private final int port;
        private final Class<M> modelClass;

        private int timeout = 2500;

        private DiscoveryHandler<M> discoveryHandler;


        public Builder(int port, Class<M> modelClass) {
            this.port = port;
            this.modelClass = modelClass;
        }

        public Builder<M> withTimeout(int timeout){
            this.timeout = timeout;
            return this;
        }

        public Builder<M> withDiscoveryHandler(DiscoveryHandler<M> discoveryHandler){
            this.discoveryHandler = discoveryHandler;
            return this;
        }

        @Override
        public DiscoveryAgent<M> build() {
            return new DiscoveryAgent<>(modelClass, port, timeout, discoveryHandler, errorHandler, gson);
        }
    }
}
