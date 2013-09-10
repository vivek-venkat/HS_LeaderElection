public enum NodeState {
	INITIALIZING, READY, LIMBO, LEADER;
	public static NodeState fromInt(int x) {
		switch (x) {
		case 0:
		default:
			return READY;
		case 1:
			return LIMBO;
		case 2:
			return LEADER;
		}
	}
}
