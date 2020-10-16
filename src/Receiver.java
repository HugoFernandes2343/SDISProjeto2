import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.Arrays;

public class Receiver implements Runnable {
    private int port;
    private SSLServerSocket serverSocket;

    public Receiver(int port){
        this.port = port;

        System.setProperty("javax.net.ssl.keyStore", "server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        System.setProperty("javax.net.ssl.trustStore", "truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        SSLServerSocketFactory serverSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        try {
            serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
            serverSocket.setNeedClientAuth(true);
            serverSocket.setEnabledProtocols(serverSocket.getSupportedProtocols());
            System.out.println("Server socket thread created and ready to receive");
        } catch (IOException e) {
            System.err.println("Error creating server socket");
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        SSLSocket connectionSocket = null;
        while(true) {
            try {
                connectionSocket = (SSLSocket) serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream inFromClient = null;
            try {
                inFromClient = connectionSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            DataInputStream in = new DataInputStream(inFromClient);
            if(connectionSocket != null){
                if(inFromClient != null) {
                    byte[] buffer = new byte[64000];
                    byte[] data = new byte[64000];
                    try {
                        int readsize = in.read(buffer);
                        data = Arrays.copyOfRange(buffer, 0, readsize);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Peer.executor.execute(new MessageManagement(data));
                }
            }
        }
    }
}
