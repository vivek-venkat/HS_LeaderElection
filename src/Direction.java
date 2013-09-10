public enum Direction {
	RIGHT, LEFT;
	Direction change() {
		return (this == LEFT) ? RIGHT : LEFT;
	}
}
