import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Storage implements Serializable {

    /**
     * key = fileID
     * index = chunkNo value = chunkData
     */
    private Map<String, ArrayList<byte[]>> chunks;
    /**
     * key = fileID
     * ReplicationDegree
     */
    public Map<String,Integer> repDegree;
    /**
     * key = fileID
     * index = chunkNo value = chunkData
     */
    private Map<String, ArrayList<byte[]>> restoredChunks;
    /**
     * key = fileID
     * replication degree
     */
    public Map<String, Integer> backedUp;

    public int spaceAvailable;
    public int totalSpace;


    public Storage(){
        this.totalSpace = 1000000;
        this.spaceAvailable = totalSpace;
        chunks=new HashMap<>();
        restoredChunks= new HashMap<>();
        repDegree=new HashMap<>();
        backedUp=new HashMap<>();
    }

    //true if file was created. False if it is still waiting for chunks
    public boolean addChunk(String fileId, int index, byte[] data,int maxChunks,int desiredRP){
        //if the fileid is in the map add that chunk to the Array
        if(chunks.containsKey(fileId)) {
        }
        else{
            //if the fileis isnt in the map add a new entry to the map with a null filled array and the fileid
            ArrayList<byte[]> temp = new ArrayList<>();
            for(int i = 0; i<maxChunks; i++)
                temp.add(null);
            chunks.put(fileId,temp);
        }
        chunks.get(fileId).set(index,data);

        //checks if any of the chunks is still missing if there is any missing stops the method
        for(int i = 0; i<chunks.get(fileId).size();i++){
            if(chunks.get(fileId).get(i)==null){
                return false;
            }
        }

        //if the array is filed it starts to create the file
        byte[] fileData = new byte[0];
        ByteArrayOutputStream outputMessageStream = new ByteArrayOutputStream();
        for (int i = 0; i < chunks.get(fileId).size(); i++) {
             try {
                outputMessageStream.write(Arrays.copyOf(fileData, fileData.length));
                outputMessageStream.write(Arrays.copyOf(chunks.get(fileId).get(i), chunks.get(fileId).get(i).length));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte[] data2 = outputMessageStream.toByteArray();
        try {
            FileOutputStream streamToFile = new FileOutputStream("./fileSystem/" + Peer.getID() + "/backup/" + fileId);
            streamToFile.write(data2);
            streamToFile.close();
        } catch (IOException e)
        {
            System.out.println("Could not Create file");
        }
        repDegree.put(fileId, desiredRP);
        addToSystemReplications(fileId);
        spaceAvailable-=data2.length;
        return true;
    }

    public void addToSystemReplications(String tag) {
        if(!backedUp.containsKey(tag)){
            backedUp.put(tag,1);
        }else{
            int current = backedUp.get(tag);
            backedUp.replace(tag,current+1);
        }
    }

    public void subtracToSystemReplications(String tag, int rep) {
        if(backedUp.containsKey(tag)){
            int current = backedUp.get(tag);
            backedUp.replace(tag,current-1);
        }
        else backedUp.put(tag,rep);
    }

    public  Map<String, ArrayList<byte[]>> getRestoredChunks(){
        return restoredChunks;
    }

    public boolean addRestoredChunk(String fileId, String fileName, int index, byte[] data,int maxChunks){
        //if the fileid is in the map add that chunk to the Array
        if(restoredChunks.containsKey(fileId)) {
        }
        else{
            //if the fileis isnt in the map add a new entry to the map with a null filled array and the fileid
            ArrayList<byte[]> temp = new ArrayList<>();
            for(int i = 0; i<maxChunks; i++)
                temp.add(null);
            restoredChunks.put(fileId,temp);
        }
        restoredChunks.get(fileId).set(index,data);

        //checks if any of the chunks is still missing if there is any missing stops the method
        for(int i = 0; i<restoredChunks.get(fileId).size();i++){
            if(restoredChunks.get(fileId).get(i)==null){
                return false;
            }
        }

        //if the array is filed it starts to create the file
        File temp = new File("./fileSystem/" + Peer.getID() + "/restore/" + fileName);
        if(!temp.exists()) {
            byte[] fileData = new byte[0];
            ByteArrayOutputStream outputMessageStream = new ByteArrayOutputStream();
            for (int i = 0; i < restoredChunks.get(fileId).size(); i++) {
                try {
                    outputMessageStream.write(Arrays.copyOf(fileData, fileData.length));
                    outputMessageStream.write(Arrays.copyOf(restoredChunks.get(fileId).get(i), restoredChunks.get(fileId).get(i).length));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            byte[] data2 = outputMessageStream.toByteArray();
            try {
                FileOutputStream streamToFile = new FileOutputStream("./fileSystem/" + Peer.getID() + "/restore/" + fileName);
                streamToFile.write(data2);
                streamToFile.close();
            } catch (IOException e) {
                System.out.println("Could not Create file");
            }
        }
        return true;
    }

    public int getReplicationsInSystem(String tag) {
        if(repDegree.containsKey(tag)){
            return repDegree.get(tag);
        }
        return 0;
    }


}
