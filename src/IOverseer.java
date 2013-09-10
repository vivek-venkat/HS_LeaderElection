

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IOverseer extends Remote {
	
	int REG_PORT = 3334;
	void nodeBound(String nodeID) throws RemoteException;
	void reportInfo(String message) throws RemoteException;
	void reportException(String nodeID, Exception e) throws RemoteException;
	void reportNodeDeath(String nodeID) throws RemoteException;
	void reportCount(int messageCount) throws RemoteException;
	void ringDestroyed() throws RemoteException;
}
