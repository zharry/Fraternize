// Jacky Liao and Harry Zhang
// October 20, 2017
// Summative
// ICS4U Ms.Strelkovska

package network.packet;

public class PacketPlayerJoin extends Packet {

	public String playerName;

	public PacketPlayerJoin(String playerName) {
		this.playerName = playerName;
	}
}