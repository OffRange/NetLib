package de.davis.net.handlers;

import de.davis.net.sync.Client;

public interface ErrorHandler {

    void onErrorOccurred(Client client, Exception exception, Type type);

    enum Type{
        /**
         * Error occurred while disconnecting.
         */
        DISCONNECT,

        /**
         * Error occurred while sending data to the server.
         */
        SEND,

        /**
         * Error occurred while receiving data from the server.
         */
        RECEIVE,


        /**
         * Error occurred while canceling the {@link de.davis.net.sync.udp.DiscoveryAgent}.
         */
        UDP_CANCEL,

        /**
         * Error occurred while discovering an endpoint.
         */
        UDP_DISCOVERING
    }
}
