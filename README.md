# NetLib

NetLib is a Java library that includes a UDP client for discovering UDP servers on the local network and a TCP client secured with TLS.

## Usage

This library is designed to be used as a component of my [Password Manager](https://github.com/OffRange/PasswordManager) project to enable synchronization between a desktop application and an Android phone when connected to the same network. However, you are welcome to utilize it for your own purposes as well.

### TCP Client

#### Create a Custom Model

To use the TCP client effectively, you need to define custom models that represent the data you want to send or receive from the server. For instance, let's create a sample custom model:

```java
@ModelType(type = 1)
public class CustomModel extends Model {

    private String text;

    public CustomModel(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
```

The client will use this model to deserialize any JSON response coming from a server. The serialized JSON string would look like: `{"text": "some text here", "type": 1}`. The `type` key is used to identify which model is being received or sent. Therefore, if the server sends a JSON file with a `type` of `23`, you must have created a `Model` annotated with `@ModelType(type = 23)`.

#### Create a `TcpClient` Instance

To interact with the server using TCP, you can create a `TcpClient` instance as follows:

```java
TcpClient tcpClient = new TcpClient.Builder("hostname", 5555)
        .withAcceptedFingerprints("all", "cert_fingerprint1", "cert_fingerprint2")
        .withServerResponseHandler((client, model) -> {
            /*Handle your custom Model here*/
            if (model instanceof CustomModel) {
                // Your logic here
            }
        })
        .withErrorHandler((client, exception, type) -> {
            // Handle errors
        })
        .build();

tcpClient.start();

tcpClient.send(new CustomModel("Sample Text"));

int latency = tcpClient.getLatency(2500); // Gets the ping to the server with a timeout of 2500 ms. If the timeout is reached, this will return -1

tcpClient.close();
```

### Discover a UDP Server (UDP Client)

#### Create a Custom UdpModel

For the UDP client, you also need to define custom models to represent the data sent from the server. Let's create a sample custom UDP model:

```java
public class CustomUdpModel extends UdpModel {

    private String text;
    private Client client;

    private CustomUdpModel(Client client) {
        this(client, null);
    }

    public CustomUdpModel(Client client, String text) {
        this.client = client;
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
```

The UDP server would respond with a JSON format like `{"text": "some text here"}`.

#### Create a `DiscoveryAgent` Instance

To discover UDP servers on the network, you can create a `DiscoveryAgent` instance:

```java
DiscoveryAgent<CustomUdpModel> discoveryAgent = new DiscoveryAgent.Builder<>(5556, CustomUdpModel.class)
        .withErrorHandler((client, exception, type) -> {
            // Handle errors
        })
        .withTimeout(2500) // default: 2500
        .withDiscoveryHandler(new DiscoveryHandler<CustomUdpModel>() {
            public void onServerDiscovered(CustomUdpModel model, DatagramPacket packet) {
                // Agent found a server, handle it here
            }

            public void onFinish() {
                // Agent could not find more servers
            }
        })
        .build();

discoveryAgent.start();
```

Feel free to utilize the NetLib library to enhance your own projects or contribute to its development! If you have any questions or encounter issues, please don't hesitate to reach out.