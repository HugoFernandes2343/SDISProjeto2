import java.io.FileNotFoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIInterface extends Remote {
    void backupProtocol(String pathname, int replicationDegree) throws RemoteException;
    void restoreProtocol(String file) throws RemoteException;
    void deleteProtocol(String pathName) throws RemoteException;
    void reclaimProtocol(int newStorageSpace)throws RemoteException;
    void stateProtocol() throws RemoteException;
    void saveProtocol() throws RemoteException;
}
