// Jacky Liao and Harry Zhang
// Jan 18, 2017
// Summative
// ICS4U Ms.Strelkovska

package network.server;

import network.Connection;
import network.packet.Packet;

// A packet coming from a client
public class IncomingPacket {

	public Connection connection;
	public Packet packet;

	public IncomingPacket(Connection connection, Packet packet) {
		this.connection = connection;
		this.packet = packet;
	}
}