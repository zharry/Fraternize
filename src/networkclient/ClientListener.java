package networkclient;

import com.jmr.wrapper.common.Connection;
import com.jmr.wrapper.common.listener.SocketListener;

public class ClientListener implements SocketListener {

	public void received(Connection con, Object object) {
		if (object instanceof packet.Ping) {
			long diff = System.currentTimeMillis() - ((packet.Ping) object).getTime();
			NetworkUtil.log(con, "Ping Recieved! Ping: " + diff + "ms");
		}

	}

	public void connected(Connection con) {
		NetworkUtil.log(con, "Connected!");
	}

	public void disconnected(Connection con) {
		NetworkUtil.log(con, "Disconnected!");
	}

}
