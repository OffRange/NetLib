package de.davis.net.handlers;

import de.davis.net.sync.models.Model;
import de.davis.net.sync.tcp.TcpClient;

public interface ServerResponseHandler {

    void onServerResponse(TcpClient client, Model model);
}
