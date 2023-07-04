package de.davis.net.sync.tcp;

import com.google.gson.Gson;
import de.davis.net.sync.AbstractBuilder;
import de.davis.net.sync.Client;
import de.davis.net.sync.models.Model;
import de.davis.net.handlers.ErrorHandler;
import de.davis.net.handlers.ServerResponseHandler;
import de.davis.net.sync.tcp.ssl.TrustManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class TcpClient implements Client {

    private final Gson gson;

    private SSLSocket socket;
    private DataOutputStream dataOutputStream;

    private final InetSocketAddress address;

    private final ServerResponseHandler serverResponseHandler;
    private final ErrorHandler errorHandler;

    private boolean running;

    private final String[] fingerprints;

    public TcpClient(InetSocketAddress address, ServerResponseHandler serverResponseHandler, ErrorHandler errorHandler, String[] fingerprints, Gson gson){
        this.gson = gson;

        this.address = address;

        this.serverResponseHandler = serverResponseHandler;
        this.errorHandler = errorHandler;

        this.fingerprints = fingerprints;
    }

    private Socket createSocket() throws KeyManagementException, IOException {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        sslContext.init(null, new TrustManager[]{new TrustManager(fingerprints)}, null);
        SSLSocketFactory factory = sslContext.getSocketFactory();

        return factory.createSocket(address.getAddress(), address.getPort());
    }

    public void start() throws IOException {
        try {
            socket = (SSLSocket) createSocket();
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }

        socket.startHandshake();

        dataOutputStream = new DataOutputStream(socket.getOutputStream());

        running = true;
        ClientThread clientThread = new ClientThread(socket.getInputStream());
        clientThread.start();
    }

    public boolean isRunning() {
        return running;
    }

    public void close(){
        if(!isRunning())
            return;

        running = false;

        try {
            socket.close();
        } catch (IOException e) {
            callErrorOccurred(e, ErrorHandler.Type.DISCONNECT);
        }
    }

    public int getLatency(int timeout) throws IOException {
        long startTime = System.currentTimeMillis();
        if(address.getAddress().isReachable(timeout)){
            return (int)(System.currentTimeMillis() - startTime);
        }

        return -1;
    }

    public void send(Model model){
        byte[] data = gson.toJson(model).getBytes();
        try {
            dataOutputStream.writeInt(data.length);
            dataOutputStream.write(data);
        } catch (Exception e) {
            callErrorOccurred(e, ErrorHandler.Type.SEND);
        }
    }

    public SSLSocket getSocket() {
        return socket;
    }

    private void callErrorOccurred(Exception e, ErrorHandler.Type type){
        if(errorHandler == null)
            return;

        if(!isRunning())
            return;

        if((socket.isInputShutdown() || socket.isOutputShutdown()) && !socket.isClosed()){
            close();
            return;
        }

        errorHandler.onErrorOccurred(this, e, type);
    }

    private class ClientThread extends Thread{

        private final DataInputStream inputStream;

        public ClientThread(InputStream inputStream) {
            this.inputStream = new DataInputStream(inputStream);
        }

        @Override
        public void run() {
            try{
                while (isRunning()){
                    byte[] data = readFully();

                    Model model = gson.fromJson(new String(data), Model.class);
                    if(serverResponseHandler == null)
                        return;

                    serverResponseHandler.onServerResponse(TcpClient.this, model);
                }
            }catch (Exception e){
                callErrorOccurred(e, ErrorHandler.Type.RECEIVE);
            }
        }

        private byte[] readFully() throws IOException {
            int length = inputStream.readInt();
            byte[] received = new byte[length];
            inputStream.read(received);
            return received;
        }
    }

    public static class Builder extends AbstractBuilder<Builder, TcpClient> {
        private final InetSocketAddress address;

        private ServerResponseHandler serverResponseHandler;

        private String[] fingerprints = {};

        public Builder(String host, int port) {
            address = new InetSocketAddress(host, port);
        }

        public Builder withServerResponseHandler(ServerResponseHandler handler){
            this.serverResponseHandler = handler;
            return this;
        }

        public Builder withAcceptedFingerprints(String... fingerprints){
            this.fingerprints = fingerprints;
            return this;
        }

        public TcpClient build(){
            return new TcpClient(address, serverResponseHandler, errorHandler, fingerprints, gson);
        }
    }
}
