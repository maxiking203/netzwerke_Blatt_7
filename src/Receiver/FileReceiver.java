package Receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import Package.Package;
import Sender.SenderMsg;
import Sender.SenderState;


public class FileReceiver {
	
	private ReceiverState currentState;
	private Transition[][] trans = new Transition[ReceiverState.values().length][ReceiverMsg.values().length];
	
	{
		trans[ReceiverState.WAIT.ordinal()][ReceiverMsg.recieved.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return ReceiverState.CHECK_DATA;
		};
		
		trans[ReceiverState.CHECK_DATA.ordinal()][ReceiverMsg.data_right.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return ReceiverState.SEND_ACK_true;
		};
		
		trans[ReceiverState.CHECK_DATA.ordinal()][ReceiverMsg.data_wrong.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return ReceiverState.SEND_ACK_FALSE;
		};
		
		trans[ReceiverState.CHECK_DATA.ordinal()][ReceiverMsg.last_right.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return ReceiverState.GOT_LAST;
		};
		
		trans[ReceiverState.GOT_LAST.ordinal()][ReceiverMsg.done.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return ReceiverState.END;
		};
		
	}
	
	
	@FunctionalInterface
	private interface Transition {
		
		ReceiverState execute(Package p);
		
	}
	
	public static void main(String[] args) throws SocketException {
		
		DatagramSocket sock = new DatagramSocket(5000);
		sock.setSoTimeout(5000);
		
		while(true) {
			byte[] buffer = new byte[1400];
			DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
			
			try {
				sock.receive(incomingPacket);
				Package pak = new Package(incomingPacket);
				System.out.println("Package erhalten");
			}
			catch (SocketTimeoutException s) {
				//nochmal senden lassen
				System.out.println("Timeout");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
		
}
