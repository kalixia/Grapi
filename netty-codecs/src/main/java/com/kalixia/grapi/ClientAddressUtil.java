package com.kalixia.grapi;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ClientAddressUtil {

    public static String extractClientAddress(SocketAddress remoteAddress) {
        String clientAddress;
        if (remoteAddress instanceof InetSocketAddress)
            clientAddress = ((InetSocketAddress) remoteAddress).getHostName();
        else
            clientAddress = remoteAddress.toString();
        return clientAddress;
    }

}
