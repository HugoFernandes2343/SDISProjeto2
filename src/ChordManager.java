import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ChordManager implements Runnable{
    private final static int BITS = 8;
    public static BigInteger peerHash;
    private static ArrayList<String> fingerTable = new ArrayList<>(BITS);
    public static String predecessor =null;

    public ChordManager(){
        try {
            setChord();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> getFingerTable() {
        return fingerTable;
    }
    public static String getPredecessor() {
        return predecessor;
    }
    public static int getBits(){
        return BITS;
    }

    private void setChord() throws UnknownHostException {
        String [] params = new String[] {String.valueOf(Peer.port), InetAddress.getLocalHost().getHostAddress()};
        ChordManager.peerHash = encrypt(params);
        System.out.println("Peer hash = " + peerHash + "\n");

        (new File("fileSystem/"+Peer.getID()+"/backup")).mkdirs();
        (new File("fileSystem/"+Peer.getID()+"/restore")).mkdirs();

        FingerTable();
        printFingerTable();

        FixFingers ff = new FixFingers();
        Peer.executor.scheduleAtFixedRate(ff,0,500, TimeUnit.MILLISECONDS);
    }

    private void FingerTable() throws UnknownHostException {
        fingerTable.add(peerHash.toString() + " " + InetAddress.getLocalHost().getHostAddress() + " " + Peer.port);
    }

    public static void printFingerTable() {
        System.out.println("FingerTable");

        for (int i = 0; i < fingerTable.size(); i++)
        {
            String[] content =fingerTable.get(i).split(" ");
            System.out.println(i + " - " + content[0] + " - " + content[1] + " - " + content[2]);
        }

    }

    public static String calculateNextKey(BigInteger hash, int index, int m)
    {
        BigInteger add = new BigInteger(String.valueOf((int) Math.pow(2, index)));
        BigInteger mod =  new BigInteger(String.valueOf((int) Math.pow(2, m)));

        BigInteger res = hash.add(add).mod(mod);
        return res.toString();
    }

    public static String searchSuccessor(String senderInfo)
    {
        BigInteger successorKey = new BigInteger(fingerTable.get(0).split(" ")[0]);

        if(numberInInterval(peerHash, successorKey, new BigInteger(senderInfo.split(" ")[0]))){
            return "SUCESSOR " + fingerTable.get(0).split(" ")[0] + " " + fingerTable.get(0).split(" ")[1] +
                    " " + fingerTable.get(0).split(" ")[2] + " " +senderInfo.split(" ")[0] + " "+
                    senderInfo.split(" ")[1] + " " + senderInfo.split(" ")[2];
        }
        else {
            for(int i = fingerTable.size()-1; i >= 0; i--){

                if(fingerTable.get(i).split(" ")[0] == null){
                    continue;
                }

                if(numberInInterval(peerHash, new BigInteger(senderInfo.split(" ")[0]), new BigInteger(fingerTable.get(i).split(" ")[0]))) {

                    if(fingerTable.get(i).split(" ")[0].equals(ChordManager.peerHash)){
                        continue;
                    }

                    return "LOOKUP " + senderInfo.split(" ")[0] + " " +senderInfo.split(" ")[1] + " " + senderInfo.split(" ")[2] + " "
                    + null + " " + fingerTable.get(i).split(" ")[1] + " " + fingerTable.get(i).split(" ")[2];
                }
            }

            try {
                return "SUCESSOR " + ChordManager.peerHash.toString() + " " + InetAddress.getLocalHost().getHostAddress() +
                        " " + Peer.port + " " + senderInfo.split(" ")[0] + " "+ senderInfo.split(" ")[1] + " " + senderInfo.split(" ")[2];
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static BigInteger encrypt(String[] params)
    {
        String str = "";
        MessageDigest md = null;
        StringBuilder res = new StringBuilder();

        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error with MessageDigest");
            System.exit(1);
        }

        for(int i = 0; i < params.length; i++) {
            str += params[i];

            if(i < params.length - 1){
                str += " ";
            }
        }

        md.update(str.getBytes());
        byte[] hashBytes = md.digest();

        byte[] trimmedHashBytes = Arrays.copyOf(hashBytes, BITS/8);

        for (byte byt : trimmedHashBytes) {
            res.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        }

        long resLong = convertToDec(res.toString());

        return BigInteger.valueOf(resLong);
    }

    public static boolean numberInInterval(BigInteger begin, BigInteger end, BigInteger value)
    {
        int cmp = begin.compareTo(end);
        boolean bool = false;

        if(cmp == 1) {
            if (value.compareTo(begin) == 1 || value.compareTo(end) == -1){
                bool = true;
            }
        }else if (cmp == -1) {
            if (value.compareTo(begin) == 1 && value.compareTo(end) == -1){
                bool = true;
            }
        }else {
            if (value.compareTo(begin) == 0){
                bool = true;
            }
        }

        return bool;
    }

    private static long convertToDec(String hex)
    {
        String hexadecimal = "0123456789ABCDEF";
        hex = hex.toUpperCase();
        long val = 0;
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            int d = hexadecimal.indexOf(c);
            val = 16 * val + d;
        }
        return val;
    }

    @Override
    public void run() {

    }
}
