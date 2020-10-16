import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Peer implements RMIInterface {

    private static Storage storage;
    private static int id;

    private static String ip;

    static {
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static int port=0;
    private static int connectionPort=0;
    private static BigInteger hashedKey=new BigInteger(String.valueOf(0));

    private static Peer instance = null;

    public static ScheduledExecutorService executor;

    private static void initiate(String[] args){
        if (args.length >4 || args.length<2) {
            System.out.println("Usage: java Peer" +
                    "<peer_id> <peer_port> <connection_port> <connection_ip>");
            return;
        }

        id = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        if(args.length>=3) {
            connectionPort = Integer.parseInt(args[2]);
        }
        else connectionPort=-0;
        if(args.length>=4){
            ip = args[3];
        }
        else ip = null;

        Runnable receiver = new Receiver(port);
        executor.execute(receiver);

        if(!readStorageSave()) storage = new Storage();
        System.out.println("Peer connected on port " + port);
    }

    public static void main(String args[]) throws IOException {
        executor = Executors.newScheduledThreadPool(100);

        initiate(args);

        ChordManager ci = new ChordManager();
        executor.submit(ci);

        if (connectionPort != 0) {
            Runnable sendMessage = new SendMessageThread("LOOKUP " + ChordManager.peerHash + " " + InetAddress.getLocalHost().getHostAddress() +
                    " " + port + " " + null + " " + ip + " " + connectionPort);
            executor.execute(sendMessage);

        }

        executor.scheduleAtFixedRate(new Stabilize(), 0, 500, TimeUnit.MILLISECONDS);

        instance = new Peer();

        try {
            RMIInterface stub = (RMIInterface) UnicastRemoteObject.exportObject(instance, 0);
            try {
                Registry reg = LocateRegistry.getRegistry();
                reg.rebind(Integer.toString(id), stub);
                System.out.println("peer.Peer connected through getRegistry");
            } catch (Exception e) {
                Registry reg = LocateRegistry.createRegistry(1099);
                reg.rebind(Integer.toString(id), stub);
                System.out.println("peer.Peer connected through createRegistry");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public static Storage getStorage() {
        return storage;
    }

    @Override
    public void backupProtocol(String pathname, int replicationDegree) {

        //MESSAGE FORMAT -> <TYPE> <FILE_ID> <REPDEGREE> <CHUNK_NO> <CHUNK_MAX> <SENDER_PORT> <CRLF> <CRLF> <DATA>
        File f = new File("files/" + pathname);
        String fileId = Util.sha256(f.getName() + f.lastModified());
        if(f.length()<=storage.spaceAvailable) {
            storage.backedUp.put(fileId, replicationDegree);
            try {
                //send message
                ArrayList<String> fingerTable = ChordManager.getFingerTable();
                ArrayList<Integer> usedFingers = new ArrayList<>(fingerTable.size());
                int i = 0;
                while (i < fingerTable.size()) {
                    if (storage.getReplicationsInSystem(fileId) >= replicationDegree)
                        break;
                    int targetPort = Integer.parseInt(fingerTable.get(i).split(" ")[2]);
                    if (!usedFingers.contains(targetPort)) {
                        FileInputStream fis = new FileInputStream(f);
                        BufferedInputStream bst = new BufferedInputStream(fis);
                        int chunkMax = (int) Math.ceil((float) f.length() / (float) (1000 * 16));
                        byte[] buff = new byte[1000 * 16];
                        int j;
                        int chunkNo = 0;
                        while ((j = bst.read(buff)) > 0) {
                            byte[] chunk = Arrays.copyOf(buff, j);

                            //putfile message
                            String header = "PUTFILE " + fileId + " " + replicationDegree + " " + chunkNo + " " + chunkMax + " " + Peer.port;

                            Runnable sendMessageThread =new SendMessageThread(header, chunk, targetPort);
                            executor.execute(sendMessageThread);
                            chunkNo++;
                        }
                        usedFingers.add(targetPort);
                    }
                    i++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void restoreProtocol(String fileName){
        File f = new File("files/"+ fileName);
        String fileId = Util.sha256(f.getName() + f.lastModified());

        File backed = new File("./fileSystem/" + Peer.getID() + "/backup/" + fileId);
        if(backed.exists()){
            File restored = new File("./fileSystem/" + Peer.getID() + "/restore/" + fileName);
            try {
                Files.copy(backed.toPath(), restored.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            ArrayList<Integer> usedFingers = new ArrayList<>(ChordManager.getFingerTable().size());
            int i = 0;
            while (i < ChordManager.getFingerTable().size()) {
                int targetPort = Integer.parseInt(ChordManager.getFingerTable().get(i).split(" ")[2]);
                if (!usedFingers.contains(targetPort)) {
                    Runnable sendMessageThread =new SendMessageThread("GETFILE " + fileId + " " + fileName + " " + Peer.port,targetPort);
                    executor.execute(sendMessageThread);
                    usedFingers.add(targetPort);
                }
                i++;

            }
        }
    }

    @Override
    public void deleteProtocol(String pathName) {
        File f = new File("files/" + pathName);
        String fileId = Util.sha256(f.getName() + f.lastModified());
        File backed = new File("./fileSystem/" + Peer.getID() + "/backup/" + fileId);
        if(backed.exists()) backed.delete();
        if(storage.backedUp.containsKey(fileId)) {
            storage.backedUp.remove(fileId);
        }
        else{
            System.out.println("This peer did not backup the specified file");
        }
        //message
        String header = "DELETE " + fileId + " " + Peer.port + " " + (char) 0xD + (char) 0xA + (char) 0xD + (char) 0xA;

        ArrayList<String> fingerTable = ChordManager.getFingerTable();
        ArrayList<Integer> usedFingers = new ArrayList<>(fingerTable.size());
        int i = 0;
        while (i < fingerTable.size()) {
            int targetPort = Integer.parseInt(fingerTable.get(i).split(" ")[2]);
            if (!usedFingers.contains(targetPort)) {
                Runnable sendMessageThread =new SendMessageThread(header, targetPort);
                executor.execute(sendMessageThread);
                usedFingers.add(targetPort);
            }
            i++;
        }
        storage.backedUp.remove(fileId);
    }

    @Override
    public void reclaimProtocol(int newStorageSpace) {
        //checks if the current space is smaller then the new storage space
        if(newStorageSpace >= storage.totalSpace){
            storage.spaceAvailable+=newStorageSpace-storage.totalSpace;
            storage.totalSpace=newStorageSpace;
        }else{
            storage.spaceAvailable=newStorageSpace-(storage.totalSpace-storage.spaceAvailable);
            if(0 < storage.spaceAvailable){
                storage.totalSpace=newStorageSpace;
            }else {
                System.out.println("Space needed: " + Math.abs(storage.spaceAvailable));
                //in case of needing to remove files the storage will remove the oldest files first until there is enough space
                ArrayList<File> filesToDelete = new ArrayList<>();
                Set<String> map = storage.repDegree.keySet();
                for (String fileId : map) {
                    if (0 < storage.spaceAvailable) {
                        break;
                    } else {
                        File backed = new File("./fileSystem/" + Peer.getID() + "/backup/" + fileId);
                        reclaimBackup(fileId);
                        filesToDelete.add(backed);
                        storage.spaceAvailable += backed.length();
                        if (storage.spaceAvailable > storage.totalSpace) storage.spaceAvailable = storage.totalSpace;
                        try {
                            Thread.sleep((long) (Math.random() * 400));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                storage.totalSpace = newStorageSpace;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (File file : filesToDelete) {
                    file.delete();
                }
            }
        }
        System.out.println("New storage available space is:" + storage.spaceAvailable);

    }

    private void reclaimBackup(String fileId){
        //MESSAGE FORMAT -> <TYPE> <FILE_ID> <REPDEGREE> <CHUNK_NO> <CHUNK_MAX> <SENDER_PORT> <CRLF> <CRLF> <DATA>
        File f = new File("fileSystem/" + Peer.id + "/backup/" + fileId);
        if(f.length()>=storage.spaceAvailable) {
            try {
                //send message
                ArrayList<String> fingerTable = ChordManager.getFingerTable();
                ArrayList<Integer> usedFingers = new ArrayList<>(fingerTable.size());
                int i = 0;
                while (i < fingerTable.size()) {
                    if (storage.getReplicationsInSystem(fileId) > storage.repDegree.get(fileId)) {
                        break;
                    }
                    int targetPort = Integer.parseInt(fingerTable.get(i).split(" ")[2]);
                    if (!usedFingers.contains(targetPort)) {
                        usedFingers.add(targetPort);
                        FileInputStream fis = new FileInputStream(f);
                        BufferedInputStream bst = new BufferedInputStream(fis);
                        int chunkMax = (int) Math.ceil((float) f.length() / (float) (1000 * 16));
                        byte[] buff = new byte[1000 * 16];
                        int j;
                        int chunkNo = 0;
                        while ((j = bst.read(buff)) > 0) {
                            byte[] chunk = Arrays.copyOf(buff, j);

                            //putfile message
                            String header = "PUTFILE " + fileId + " " + storage.repDegree.get(fileId) + " " + chunkNo + " " + chunkMax + " " + Peer.port;
                            Runnable sendMessageThread =new SendMessageThread(header, chunk, targetPort);
                            executor.execute(sendMessageThread);
                            chunkNo++;
                        }
                        Runnable sendMessageThreadRemoved =new SendMessageThread("REMOVED " + fileId +  " " + (Peer.getStorage().getReplicationsInSystem(fileId)-1), targetPort);
                        executor.execute(sendMessageThreadRemoved);
                        fis.close();
                        bst.close();
                    }
                    i++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stateProtocol() {
        //todo?
    }

    @Override
    public void saveProtocol(){
        saveStorage();
    }

    /**
     * read from serializable file
     */
    private static boolean readStorageSave() {

        String filepath = "fileSystem/" + port + "/storageSavefile.ser";
        File saveFile = new File("fileSystem/" + port + "/storageSavefile.ser");

        //if the savefile doesnt exist just let the storage stay as empty
        if(!saveFile.exists()){
            return false;
        }

        try {
            FileInputStream fileInputStream = new FileInputStream(filepath);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            storage = (Storage) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
            System.out.println("The Object  was successfully read from a file");
            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;

    }
    /**
     * Save serializable object
     */
    private static void saveStorage() {
        try {
            String filepath = "./fileSystem/" + port + "/storageSavefile.ser";

            FileOutputStream fileOut = new FileOutputStream(filepath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(storage);
            objectOut.close();
            fileOut.close();
            System.out.println("The Object  was succesfully written to a file");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static int getID() {
        return id;
    }
}