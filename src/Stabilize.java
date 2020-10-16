import java.net.InetAddress;

public class Stabilize implements Runnable {
    @Override
    public void run() {
            try {
                if(ChordManager.getFingerTable().get(0).split(" ")[2].equals(Integer.toString(Peer.port)) && ChordManager.predecessor != null){
                    ChordManager.getFingerTable().set(0, ChordManager.predecessor);
                } else if(!ChordManager.getFingerTable().get(0).split(" ")[2].equals(Integer.toString(Peer.port))){
                    Runnable sendMessageThread = new SendMessageThread("GETPREDECESSOR " + null + " " + InetAddress.getLocalHost().getHostAddress() + " " + Peer.port + " " +
                            null +" " +  ChordManager.getFingerTable().get(0).split(" ")[1] + " " + ChordManager.getFingerTable().get(0).split(" ")[2]);
                    Peer.executor.execute(sendMessageThread);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
