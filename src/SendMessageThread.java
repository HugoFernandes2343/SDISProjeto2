import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;


public class SendMessageThread implements Runnable{

    String message;
    int port;
    byte[] chunk;

    SendMessageThread(String message, int port)
    {
        this.message = message + " " + (char) 0xD + (char) 0xA + (char) 0xD + (char) 0xA;
        this.port=port;
        System.out.println("Sending " + message.split(" ")[0] + " to port " + port);
    }
    SendMessageThread(String message)
    {
        this.message = message += " " + (char) 0xD + (char) 0xA + (char) 0xD + (char) 0xA;
        this.port=Integer.parseInt(message.split(" ")[6]);
        System.out.println("Sending " + message.split(" ")[0] + " to port " + message.split(" ")[6]);
    }

    SendMessageThread(String message, byte[] chunk, int port) {
        this.chunk=chunk;
        this.message = message + " " + (char) 0xD + (char) 0xA + (char) 0xD + (char) 0xA;
        this.port = port;
        System.out.println("Sending " + message.split(" ")[0] + " to port " + port);
    }


    @Override
    public void run() {
        if(port==Peer.port) return;
        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket clientSocket = null;
        try {
            clientSocket = (SSLSocket) socketFactory.createSocket(InetAddress.getLocalHost().getHostAddress(), port);
            clientSocket.startHandshake();
            OutputStream outToServer = null;
            try {
                outToServer = clientSocket.getOutputStream();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                DataOutputStream out = new DataOutputStream(outToServer);
                if (this.chunk != null) {
                    byte[] c = new byte[message.getBytes().length + chunk.length];
                    System.arraycopy( message.getBytes(), 0, c, 0, message.getBytes().length);
                    System.arraycopy(chunk, 0, c, message.getBytes().length, chunk.length);
                    out.write(c);
                }
                else{
                    out.write(message.getBytes());
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Port not available to receive message");
        }

    }
}
