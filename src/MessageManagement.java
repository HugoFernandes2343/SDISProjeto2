import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class MessageManagement  implements Runnable{

    private byte[] data;
    private String[] msgHeader;

    public MessageManagement(byte[] data) {
        this.data = data;
    }

    @Override
    public void run() {
        String s = new String(data, 0, data.length);
        msgHeader = s.split(" ");

        System.out.println("Received " + msgHeader[0]);

        switch (msgHeader[0]){
            case "PUTFILE":
                putFile();
                break;
            case "GETFILE":
                getFile();
                break;
            case "FILE":
                file();
                break;
            case "STORED":
                stored();
                break;
            case "DELETE":
                delete();
                break;
            case "REMOVED":
                removed();
                break;
            case "LOOKUP":
                lookup();
                break;
            case "SUCESSOR":
                sucessor();
                break;
            case "PREDECESSOR":
                predecessor();
                break;
            case "GETPREDECESSOR":
                getPredecessor();
                break;
            case "RESPONSEPREDECESSOR":
                responsePredecessor();
                break;
            default:
                System.out.println("TODO message handler for tyoe " + msgHeader[0]);
                break;
        }
    }

    private void putFile() {
        //MESSAGE FORMAT -> "PUTFILE "  + fileId + " " + replicationDegree + " " + chunkNo + " " + chunkMax + " " + senderPort + " " + (char) 0xD + (char) 0xA + (char) 0xD + (char) 0xA + <DATA>
        Set<String> map = Peer.getStorage().repDegree.keySet();
        for(String f: map){
            if(f.equals(msgHeader[1]))
            {
                return;
            }
        }
        //verifying if there is space for the
        byte[] data = getFileData();
        if(Peer.getStorage().totalSpace<data.length){
            System.out.println("not enough space for this file data:" + data.length/1024);
            return;
        }else {
            //save the data transmitted in the PUTFILE Message
            String fileId = msgHeader[1];
            int repDegree = Integer.parseInt(msgHeader[2]);
            int chunkNo =Integer.parseInt(msgHeader[3]);
            int chunkMax =Integer.parseInt(msgHeader[4]);
            int portOfSender = Integer.parseInt(msgHeader[5]);

            boolean fileComplete = Peer.getStorage().addChunk(fileId,chunkNo,data,chunkMax,repDegree);

            //if the file is complete send stored of file
            if(fileComplete){
                //Final part creating and sending the STORED message
                String msg = "STORED" + " " + fileId + " " + (char) 0xD + (char) 0xA + (char) 0xD + (char) 0xA;
                ArrayList<Integer> usedFingers = new ArrayList<>(ChordManager.getFingerTable().size());
                int i = 0;
                while (i < ChordManager.getFingerTable().size()) {
                    int targetPort = Integer.parseInt(ChordManager.getFingerTable().get(i).split(" ")[2]);
                    if (!usedFingers.contains(targetPort)) {
                        usedFingers.add(targetPort);
                        Thread t = new Thread(new SendMessageThread(msg, portOfSender));
                        t.start();
                        //Waiting for the random amount of milliseconds
                        try {
                            Thread.sleep((long) (Math.random() * 400));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    i++;
                }
            }else{
                System.out.println("Saved chunk number " + chunkNo + " from file " + fileId + " out of " + chunkMax + " chunks" );
            }
        }
    }

    private void stored() {
        Peer.getStorage().addToSystemReplications(msgHeader[1]);
    }

    private void getFile() {
        File fileName = new File("files/" + msgHeader[2]);
        try {
            FileInputStream f = new FileInputStream("./fileSystem/" + Peer.getID() + "/backup/" + msgHeader[1]);
            BufferedInputStream bst = new BufferedInputStream(f);
            byte[] buff = new byte[1000 * 16];
            int chunkMax = (int)Math.ceil((float)fileName.length() / (float)(1000*16));
            int j;
            int chunkNo = 0;
            while((j = bst.read(buff)) > 0){
                byte[] chunk = Arrays.copyOf(buff,j);

                //putfile message
                Thread t = new Thread(new SendMessageThread("FILE " + msgHeader[1] + " " + chunkNo + " " + msgHeader[2] + " "+  chunkMax, chunk, Integer.parseInt(msgHeader[3])));
                t.start();
                chunkNo++;
            }

        } catch (FileNotFoundException e) {
            System.out.println("Peer does not contain file");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void file() {
        byte[] chunkData= getFileData();
        Peer.getStorage().addRestoredChunk(msgHeader[1], msgHeader[3], Integer.parseInt(msgHeader[2]),chunkData, Integer.parseInt(msgHeader[4]));
    }

    private void delete() {
        // message "DELETE " + fileId + " " + Peer.port + " " + (char) 0xD + (char) 0xA + (char) 0xD + (char) 0xA;
        //getting the id of the file
        String fileId = msgHeader[1];
        int portOfSender = Integer.parseInt(msgHeader[2]);
        if(Peer.getStorage().backedUp.containsKey(fileId)) {
            Peer.getStorage().backedUp.remove(fileId);
        }
        //checking for the file
        File f = new File("fileSystem/" + Peer.getID() +"/backup/" + fileId);

        if(f.exists()){
            f.delete();
            Peer.getStorage().repDegree.remove(fileId);
        }
    }

    private void removed() {
        Peer.getStorage().subtracToSystemReplications(msgHeader[1],Integer.parseInt(msgHeader[2]));
    }

    private void lookup(){
        if(Integer.parseInt(ChordManager.getFingerTable().get(0).split(" ")[2]) == Peer.port) {
            Thread t = null;
            try {
                t = new Thread(new SendMessageThread("SUCESSOR " +ChordManager.peerHash + " " + InetAddress.getLocalHost().getHostAddress()+ " "
                        + Peer.port + " " + msgHeader[1] + " " + msgHeader[2] + " " + msgHeader[3]));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            t.start();
        } else {
            String msg = ChordManager.searchSuccessor(msgHeader[1] + " " + msgHeader[2] + " " +msgHeader[3]);
            Thread t = new Thread(new SendMessageThread(msg));
            t.start();
        }
    }

    private  void sucessor(){

        int index;

        if(msgHeader[4].equals(ChordManager.peerHash.toString())) {
            index = 0;
        } else {
            for(index = 0; index < ChordManager.getBits(); index++)
            {
                String res = ChordManager.calculateNextKey(ChordManager.peerHash, index, ChordManager.getBits());
                if(res.equals(msgHeader[4]))
                    break;
            }
        }

        ChordManager.getFingerTable().set(index,msgHeader[1] + " " + msgHeader[2] + " " +msgHeader[3]);

        if(msgHeader[4].equals(ChordManager.peerHash))
            return;

        try {
            Thread t = new Thread(new SendMessageThread("PREDECESSOR " + ChordManager.peerHash.toString() + " " + InetAddress.getLocalHost().getHostAddress() + " "
             + Peer.port + " " + null + " " + msgHeader[2] +" " + msgHeader[3]));
            t.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void predecessor(){
        if(ChordManager.predecessor == null ||
                ChordManager.numberInInterval(new BigInteger(ChordManager.predecessor.split(" ")[0]),ChordManager.peerHash, new BigInteger(msgHeader[1])));
            ChordManager.predecessor =  msgHeader[1] + " " + msgHeader[2] +" " + msgHeader[3];
    }

    private void getPredecessor(){
        Thread t = null;
        if(ChordManager.predecessor == null){
            try {
                t = new Thread(new SendMessageThread("RESPONSEPREDECESSOR " +null+" " +InetAddress.getLocalHost().getHostAddress() + " " + Peer.port
                + " "+ null +" "+msgHeader[2]+" "+msgHeader[3]));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
                t = new Thread(new SendMessageThread("RESPONSEPREDECESSOR " + ChordManager.getPredecessor().split(" ")[0] + " "
                        + ChordManager.getPredecessor().split(" ")[1] + " " + ChordManager.getPredecessor().split(" ")[2] + " "
                + null + " " + msgHeader[2] + " " + msgHeader[3]));
        }
        t.start();
    }

    private void responsePredecessor(){

        if(msgHeader[1] == null || msgHeader[1].equals("null")){
        } else if(ChordManager.numberInInterval(ChordManager.peerHash, new BigInteger(ChordManager.getFingerTable().get(0).split(" ")[0]),new BigInteger(msgHeader[1]))) {
            ChordManager.getFingerTable().set(0, msgHeader[1]+" "+msgHeader[2]+" "+msgHeader[3]);
        }
        Thread t = null;
        try {
            t = new Thread(new SendMessageThread("PREDECESSOR " + ChordManager.peerHash + " "+ InetAddress.getLocalHost().getHostAddress() + " "+Peer.port +" "
            +null + " " + msgHeader[2] + " "+msgHeader[3]));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        t.start();
    }

    public byte[] getFileData(){
        int headerLength = 0;
        for (int i = 0; i < data.length; i++) {
            if ((data[i] == (char) 0xD) && (data[i + 1] == (char) 0xA) &&
                    (data[i+2] == (char) 0xD) && (data[i + 3] == (char) 0xA)) {
                break;
            }
            headerLength++;
        }
        byte[] finished = Arrays.copyOfRange(data, headerLength + 4,data.length);
        return finished;
    }
}
