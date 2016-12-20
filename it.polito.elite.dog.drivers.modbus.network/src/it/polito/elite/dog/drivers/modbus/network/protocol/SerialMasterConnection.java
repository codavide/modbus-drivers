package it.polito.elite.dog.drivers.modbus.network.protocol;

import net.wimpi.modbus.net.MasterConnection;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

/**
 * 
 * Wrapper class to manage a {@link SerialConnection} as a {@link MasterConnection}
 * 
 * @author <a href="mailto:davide.conzon@ismb.it">Davide Conzon</a> 
 *
 * @since 13 Jan 2016
 */
public class SerialMasterConnection implements MasterConnection {

	private SerialConnection connection;
	
	public SerialMasterConnection(SerialParameters serialParameters) {
		connection = new SerialConnection(serialParameters);
	}
	
	@Override
	public void close() {
		connection.close();
	}

	@Override
	public void connect() throws Exception {
		connection.open();
	}

	@Override
	public boolean isConnected() {
		return connection.isOpen();
	}

	/**
	 * @return the {@link SerialConnection} wrapped
	 */
	public SerialConnection getConnection() {
		return connection;
	}
}
