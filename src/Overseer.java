import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Overseer extends UnicastRemoteObject implements IOverseer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6880138479009638331L;

	private int simulationSize;
	private int[] ring;
	private Node[] childStubs;
	private ArrayList<String> liveNodes = new ArrayList<String>();
	private int totalMessages = 0;

	public Overseer(int simulationSize) throws RemoteException {
		this.simulationSize = simulationSize;
		ring = new int[simulationSize];
		childStubs = new Node[simulationSize];
	}

	public static void main(String[] args) throws Exception {
		//		int simSize = Integer.parseInt(args[0]);
		int simSize = 128;
		Overseer overseer = new Overseer(simSize);
		Registry registry = LocateRegistry.createRegistry(IOverseer.REG_PORT);
		registry.rebind("overseer", overseer);
		System.out.println("Node bound");
		overseer.startSimulation();
	}

	private void startSimulation() throws Exception {
		try {
			Map<Integer, Boolean> shuffledNodes = new HashMap<Integer, Boolean>();
			int nodeIndex = 0;
			//Randomization algorithm, works better than Collections.shuffle!
			while (!(shuffledNodes.size() == simulationSize)) {
				int random = (int) (Math.random() * (simulationSize + 1));
				if (!(shuffledNodes.containsKey(random)) && random != 0) {
					shuffledNodes.put(random, true);
					ring[nodeIndex++] = random;
					liveNodes.add(String.valueOf(random));
				}
			}
			for (nodeIndex = 0; nodeIndex < simulationSize; nodeIndex++) {

				System.out.print(String.valueOf(ring[nodeIndex]) + "-->");

				int waitMillis = (int) (Math.random() * 5000);
				String left = (nodeIndex == 0) ? String
						.valueOf(ring[simulationSize - 1]) : String
						.valueOf(ring[nodeIndex - 1]);
						String right = (nodeIndex == simulationSize - 1) ? String
								.valueOf(ring[0]) : String.valueOf(ring[nodeIndex + 1]);
								ProcessBuilder builder = new ProcessBuilder("java.exe",
										"-Djava.security.policy=security.policy", "Node",
										String.valueOf(waitMillis),
										String.valueOf(ring[nodeIndex]), left, right);
								builder.directory(new File(System.getProperty("user.dir")
										+ "/bin"));
								builder.start();
			}
			System.out.println("");
		} catch (Exception exception) {
			System.out.println("Error starting ring simulation...");
			exception.printStackTrace();
		}
	}

	@Override
	public void nodeBound(String nodeID) {
		try {
			System.out.println("Node with ID " + nodeID + " is active");
			int childIndex = Integer.parseInt(nodeID);
			childStubs[childIndex] = (Node) LocateRegistry.getRegistry(3334)
			.lookup(nodeID);
		} catch (Exception exception) {
			// Code will never reach here, nodeID will always be found.
		}
	}

	@Override
	public void reportException(String nodeID, Exception e)
	throws RemoteException {
		System.out.println("Exception @ " + nodeID);
		e.printStackTrace();
	}

	@Override
	public void reportInfo(String message) throws RemoteException {
		System.out.println(message);
	}

	@Override
	public void reportNodeDeath(String nodeID) throws RemoteException {
		synchronized (Overseer.class) {
			liveNodes.remove(nodeID);
			System.out.println(nodeID + " died");
			if(liveNodes.size() == 1){
				System.out.println("And the winner is..." + liveNodes.get(0));
				System.out.println(String.valueOf(totalMessages) + " exchanged amongst " + simulationSize + " nodes ");}
		}	

	}

	@Override
	public void reportCount(int messageCount) {
		totalMessages += messageCount;
	}

	@Override
	public void ringDestroyed() {
		System.out.println(String.valueOf(totalMessages) + " exchanged amongst " + simulationSize + " nodes ");
		try {
			LocateRegistry.getRegistry(REG_PORT).unbind("overseer");
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
