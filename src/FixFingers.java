import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class FixFingers implements Runnable {

    private int index = -1;

    @Override
    public void run() {
        index++;

        if(index == ChordManager.getBits() ) {
            ChordManager.printFingerTable();
            index = 0;
        }

        String key = ChordManager.calculateNextKey(ChordManager.peerHash, index, ChordManager.getBits() );
        ArrayList<String> fingerTable = ChordManager.getFingerTable();

        if(index > (fingerTable.size() - 1)) {
            try {
                fingerTable.add(ChordManager.peerHash + " " + InetAddress.getLocalHost().getHostAddress()+ " " + Peer.port);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        String msg = null;
        try {
            msg = ChordManager.searchSuccessor(new BigInteger(key).toString() + " " + InetAddress.getLocalHost().getHostAddress() + " " + Peer.port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if(msg != null){
            if(msg.split(" ")[0].equals("SUCESSOR")) {
                fingerTable.set(index, ((msg.split(" ")[1] + " " + msg.split(" ")[2] + " " + msg.split(" ")[3])));
            }else if (msg.split(" ")[0].equals("LOOKUP"))
            {
                Peer.executor.execute(new SendMessageThread(msg));
            }
        }
    }
}
