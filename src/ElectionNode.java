


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ElectionNode extends Remote {
	void passMessage(Message message) throws RemoteException, InterruptedException;
}
