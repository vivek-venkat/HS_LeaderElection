import java.io.Serializable;
import java.util.ArrayList;


public class Message implements Serializable {
	private static final long serialVersionUID = -793756574800528545L;
	String senderUId;
	int round;
	ArrayList<String> nodesTouched = new ArrayList<String>();
	private long timestamp;
	int distance;
	MessageType type;
	Direction direction;
	
	public String getSenderUId() {
		return senderUId;
	}

	public void setSenderUId(String senderUId) {
		this.senderUId = senderUId;
	}

	public int getRound() {
		return round;
	}

	public void setRound(int phase) {
		this.round = phase;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public Direction directionOfTravel() {
		return direction;
	}

	public void setRelayer(Direction direction) {
		this.direction = direction;
	}
	
	public void addToPath(String nodeID) {
		nodesTouched.add(nodeID);
	}
	
	public Message(String senderUID, int phase, int distance, MessageType type, Direction relayer) {
		this.senderUId = senderUID;
		this.round = phase;
		this.distance = distance;
		this.type = type;
		this.direction = relayer;
		timestamp = System.currentTimeMillis();
	}
	
	public Message(Message message) {
		this(message.senderUId, message.round, message.distance, message.type, message.direction);
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[").append(senderUId).append("," + round).append("," + distance).append("," + type.toString()).append("," + direction.toString()).append("]").append(nodesTouched.toString()).append("" + timestamp);
		return buffer.toString();
	}
}
