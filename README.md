HS_LeaderElection
=================

Implementation of Hirschberg Sinclair Leadership Election Algorithm on bidirectional Ring.
Each node is uniquely identified by its fully qualified host name and lexigraphic ordering is maintained for nodes.
Built using Java RMI.

Notes:

Sockets vs. RMI:

The Socket API works on a lower layer and hence there is a lesser execution overhead. RMI has this overhead since it requires additional software support, including proxies and directory service.  If high performance is mandate then socket API is better. Sockets are also platform and language independent but RMI requires Java run-time supports. 
The RMI API provides the abstraction that enables easier software development. It does the work of creating a socket and maintaining the stream without the developer having to worry about the state of a channel. This higher level of abstraction makes it easier to debug code as well. 
We chose RMI since it would be easier to implement a new algorithm and test it without having to worry about the nitty-gritty’s of the network layer and its functioning. Also RMI functions well for rapid prototype development which allows us to implement the skeletal structure of the problem quickly and spend more time on developing the algorithm.

Implementation using RMI:
	
To simulate this algorithm every node is bound to a unique port in a single system and to maintain a separate JVM for every node we spawn every Node as a different process. Since every process behaves as a Node, the only inter-process communication is via the RMI API using message passing. 
We have a main class called Overseer that is responsible for creating the ring and positioning the nodes in the ring. These nodes are assigned a UID. The Overseer adds every node to the ring and at this time also initializes the node with information about its left and right nodes. The information is passed via Constructor. This Node class then registers itself in the registry and waits for his neighbors to register themselves.  There is a class called Message that is defined to hold the message tuples. This includes the type of the message (SEND, REPLY), UID of the sender, Phase #, Hop count and direction (LEFT/RIGHT). We also maintain a Synchronous Queue Data structure that functions like a Message Queue. Messages are picked up from the queue sequentially and processed using a separate thread handler in every Node.
Once both neighbors are registered via lookup, the node sends a SEND message to its neighbors at a distance of 2phase from it. Initially the phase is set to 0. Once all the nodes receive and process their respective messages and follow the algorithms state transition functions that send it to the appropriate state, thus deciding whether they are candidates for the next round. Once a node receives REPLY messages from both its neighbors, it qualifies for the next round. There are a number of nodes that continue on to the next round (Formula given later) and once a node receives its own SEND message back it declares itself a winner/Leader and propagates this to every other node in the ring. When an incoming UID is greater than its own the node removes itself from contention and does not participate in the next round.

Assumptions:

We have set a few constraints on how this algorithm behaves in this environment. 
•Every Node is a correct node and does not misbehave. 
•The overseer class is used to just simulate the ring and assign UIDs. It cannot play God or create chaos among the nodes.
•The size of the ring is static. This algorithm implementation does not handle the case when a new node is added to the ring at any time since adding a new node simply restarts the election process.
•A node starts the election process as soon as it is bound to the registry. This is not a real-world aspect.

