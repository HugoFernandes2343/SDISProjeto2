import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Remote;

public class TestApp {

    public static void main(String[] args) throws RemoteException, NotBoundException {

        if (args.length < 2 || args.length > 4) {
            System.out.println("Wrong usage of: java TestApp <peer_id> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        String peerAccessPoint = args[0];
        if (peerAccessPoint == null) {
            return;
        }

        Registry registry = LocateRegistry.getRegistry("localhost");
        RMIInterface rmi = (RMIInterface) registry.lookup(peerAccessPoint);

        String subprotocol = args[1];
        String op1, op2;

        if (args.length > 2){
            op1 = args[2];
        }else {
            op1 = null;
        }
        if (args.length > 3){
            op2 = args[3];
        }else{
            op2 = null;
        }

        switch (subprotocol){
            case "BACKUP":
                rmi.backupProtocol(op1, Integer.parseInt(op2));
                break;
            case "RESTORE":
                rmi.restoreProtocol(op1);
                break;
            case "DELETE":
                rmi.deleteProtocol(op1);
                break;
            case "RECLAIM":
                rmi.reclaimProtocol(Integer.parseInt(op1));
                break;
            case "STATE":
                rmi.stateProtocol();
                break;
            case "SAVE":
                rmi.saveProtocol();

        }

    }
}
