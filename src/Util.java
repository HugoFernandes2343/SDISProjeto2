import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.attribute.BasicFileAttributes;

import static java.util.Arrays.copyOfRange;


public class Util {

    public static String sha256(String fileId){


        try {
            MessageDigest dig = MessageDigest.getInstance("SHA-256");
            byte[] hash = dig.digest(fileId.getBytes("UTF-8"));

            StringBuffer hexStr = new StringBuffer();

            int pos = 0;
            while (hash.length > pos  ){
                String hex = Integer.toHexString(0xff & hash[pos]);

                if(hex.length() == 1){
                    hexStr.append('0');
                }

                hexStr.append(hex);
                pos++;
            }

            return hexStr.toString();

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

    }

    public static void CreateFile(String fileName,String fileId){
        byte[] data = new byte[0];
        ByteArrayOutputStream outputMessageStream = new ByteArrayOutputStream();
        for (int i = 0; i < Peer.getStorage().getRestoredChunks().size(); i++) {
            try {
                outputMessageStream.write(Arrays.copyOf(data, data.length));
                outputMessageStream.write(Arrays.copyOf(Peer.getStorage().getRestoredChunks().get(fileId).get(i),
                        Peer.getStorage().getRestoredChunks().get(fileId).get(i).length));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte[] data2 = outputMessageStream.toByteArray();
        try {
            FileOutputStream streamToFile = new FileOutputStream("./fileSystem/" + Peer.getID() + "/restore/" + fileName);
            streamToFile.write(data2);
            streamToFile.close();
        } catch (IOException e)
        {
            System.out.println("Could not Create file");
        }
    }
}
