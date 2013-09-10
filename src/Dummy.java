import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;


public class Dummy {
	
	public static void main(String[] args) throws RemoteException {
		LocateRegistry.createRegistry(IOverseer.REG_PORT).rebind("overseer", new Overseer(1));
	}

}
