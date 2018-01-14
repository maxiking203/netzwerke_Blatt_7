package Receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import Package.Package;



public class FileReceiver {
	
	private ReceiverState currentState;
	private Transition[][] trans = new Transition[ReceiverState.values().length][ReceiverMsg.values().length];
	
	{
		trans[ReceiverState.WAIT.ordinal()][ReceiverMsg.recieved.ordinal()] = p -> {
			System.out.println("Sending first package.");
			p = new Package();
			return ReceiverState.CHECK_DATA;
		};
		
		trans[ReceiverState.CHECK_DATA.ordinal()][ReceiverMsg.data_right.ordinal()] = p -> {
			System.out.println("Checking Data of received package...is right");
			p = new Package();
			return ReceiverState.SEND_ACK_true;
		};
		
		trans[ReceiverState.SEND_ACK_true.ordinal()][ReceiverMsg.wait_right.ordinal()] = p -> {
			System.out.println("Sending true ACK.");
			p = new Package();
			return ReceiverState.WAIT;
		};
		
		trans[ReceiverState.CHECK_DATA.ordinal()][ReceiverMsg.data_wrong.ordinal()] = p -> {
			System.out.println("Checking Data of received package...is wrong.");
			p = new Package();
			return ReceiverState.SEND_ACK_FALSE;
		};
		
		trans[ReceiverState.SEND_ACK_FALSE.ordinal()][ReceiverMsg.wait_wrong.ordinal()] = p -> {
			System.out.println("Sending false ACK.");
			p = new Package();
			return ReceiverState.WAIT;
		};
		
		trans[ReceiverState.CHECK_DATA.ordinal()][ReceiverMsg.last_right.ordinal()] = p -> {
			System.out.println("Last package. Everthing was alright");
			p = new Package();
			return ReceiverState.GOT_LAST;
		};
		
		trans[ReceiverState.GOT_LAST.ordinal()][ReceiverMsg.done.ordinal()] = p -> {
			System.out.println("Ending Process.");
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
