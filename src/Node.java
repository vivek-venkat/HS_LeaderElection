import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

public class Node extends UnicastRemoteObject implements ElectionNode {
	private static final long serialVersionUID = 3467053977730837818L;

	private String nodeID;
	private String left;
	private String right;
	// Define stub for left and right
	private NodeState nodeState;
	private IOverseer overseer;
	private ElectionNode leftStub;
	private ElectionNode rightStub;
	private int round;
	private SynchronousQueue<Message> messageQueue = new SynchronousQueue<Message>();
	private int inCount = 0;
	CountDownLatch latch = new CountDownLatch(1);
	private int messageCount = 0;

	public Node(String nodeID, String left, String right, NodeState startState)
			throws RemoteException {
		nodeState = NodeState.INITIALIZING;
		this.nodeID = nodeID;
		this.left = left;
		this.right = right;
		round = 1;
		init();
	}

	private void init() {
		try {
			Registry registry = LocateRegistry.getRegistry(IOverseer.REG_PORT);
			overseer = (IOverseer) registry.lookup("overseer");
			new Thread(new MessageGrinder()).start();
			registry.rebind(nodeID, this);
			ExecutorService executorService = Executors.newCachedThreadPool();

			Future<ElectionNode> leftResult = executorService
					.submit(new NeighborLookupTask(left));
			Future<ElectionNode> rightResult = executorService
					.submit(new NeighborLookupTask(right));
			leftStub = leftResult.get();
			rightStub = rightResult.get();
			nodeState = NodeState.READY;
			latch.countDown();
			invoke(leftStub, new Message(nodeID, 1, 1, MessageType.SEND,
					Direction.RIGHT));
			invoke(rightStub, new Message(nodeID, 1, 1, MessageType.SEND,
					Direction.LEFT));
		} catch (Exception e) {
			log(e);
		}
	}

	public static void main(String[] args) throws Exception {
		int state = Integer.parseInt(args[0]);
		new Node(args[1], args[2], args[3], NodeState.fromInt(state));
	}

	private class NeighborLookupTask implements Callable<ElectionNode> {

		private String stubID;

		NeighborLookupTask(String stubID) {
			this.stubID = stubID;
		}

		@Override
		public ElectionNode call() throws Exception {
			ElectionNode stub = null;
			boolean lookupComplete = false;
			Registry registry = null;
			registry = LocateRegistry.getRegistry(IOverseer.REG_PORT);
			while (!lookupComplete) {
				try {
					Thread.sleep(200);
					stub = (ElectionNode) registry.lookup(stubID);
					lookupComplete = true;
				} catch (Exception e) {
				}
			}
			return stub;
		}
	}

	private class MessageGrinder implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					processMessage(messageQueue.take());
				} catch (Exception e) {
					log(e);
				}
			}
		}

		private void processMessage(Message message) throws Exception {

			messageCount++;
			int myUID = Integer.parseInt(nodeID);
			int incomingUID = Integer.parseInt(message.senderUId);

			if (message.type == MessageType.ELECTED) {
				overseer.reportCount(++messageCount);
				log("killing self");
				relayElected();
				LocateRegistry.getRegistry(IOverseer.REG_PORT).unbind(nodeID);
				if (nodeState == NodeState.LEADER)
					overseer.ringDestroyed();
				System.exit(0);
				return;
			}

			// Eat up messages meant for inferior nodes if you have moved on to
			// the next round.
			if (round > message.round && myUID > incomingUID)
				return;

			switch (message.direction) {
			case LEFT:
				doAction(message, myUID, incomingUID, leftStub, rightStub);
				break;
			case RIGHT:
				doAction(message, myUID, incomingUID, rightStub, leftStub);
			}

			if (inCount == 2 && nodeState == NodeState.READY) {
				inCount = 0;
				round += 1;
				invoke(leftStub,
						new Message(nodeID, round, (int) Math.pow(2, round),
								MessageType.SEND, Direction.RIGHT));
				invoke(rightStub,
						new Message(nodeID, round, (int) Math.pow(2, round),
								MessageType.SEND, Direction.LEFT));
			}
		}

		private void doAction(Message message, int myUID, int incomingUID,
				ElectionNode leftStub, ElectionNode rightStub)
				throws RemoteException {
			switch (message.type) {
			case SEND:
				if (incomingUID > myUID) {
					if (nodeState != NodeState.LIMBO) {
						nodeState = NodeState.LIMBO;
						overseer.reportNodeDeath(nodeID);
					}
					if (message.distance > 1) {
						invoke(rightStub, createForward(message));
					} else {
						invoke(leftStub, createReply(message));
					}
				} else if (incomingUID == myUID
						&& nodeState != NodeState.LEADER) {
					nodeState = NodeState.LEADER;
					relayElected();
				}
				break;
			case REPLY:
				if (incomingUID > myUID) {
					invoke(rightStub, new Message(message));
				} else {
					inCount++;
				}
				break;
			}
		}

		private void relayElected() {
			Message message = new Message(nodeID, 1, 1, MessageType.ELECTED,
					Direction.LEFT);
			invoke(rightStub, message);
		}
	}

	private Message createForward(Message message) {
		Message msg = new Message(message);
		msg.setDistance(message.distance - 1);
		return msg;
	}

	private Message createReply(Message message) {
		Message reply = new Message(message);
		reply.setDistance(1);
		reply.setType(MessageType.REPLY);
		reply.setRelayer(message.direction.change());
		return reply;
	}

	@Override
	public void passMessage(Message message) throws RemoteException,
			InterruptedException {
		latch.await();
		messageQueue.put(message);
	}

	private void log(String message) {
		try {
			overseer.reportInfo(nodeID + "--->" + message);
		} catch (RemoteException e) {
		}
	}

	private void log(Exception e) {
		try {
			overseer.reportException(nodeID, e);
		} catch (RemoteException e1) {
		}
	}

	private void invoke(final ElectionNode stub, final Message message) {
		new Thread(new Runnable() {
			public void run() {
				try {
					message.addToPath(nodeID);
					stub.passMessage(message);
				} catch (Exception e) {
					log(e);
				}
			}
		}).start();
	}
}