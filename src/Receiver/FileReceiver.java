package Receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import Package.Package;



public class FileReceiver {
	
	private DatagramSocket sock;
	private String filename;
	private final int port = 5000;
	private InetAddress ip;
	private DatagramPacket backupDataPacket;
	private int seq = 0;
	private static boolean fin = false;
	private ReceiverState currentState;
	private Transition[][] trans = new Transition[ReceiverState.values().length][ReceiverMsg.values().length];
	
	{
		trans[ReceiverState.WAIT.ordinal()][ReceiverMsg.received.ordinal()] = p -> {
			System.out.println("Sending first package.");
			p = new Package();
			return ReceiverState.SEND_ACK;
		};
		
		trans[ReceiverState.SEND_ACK.ordinal()][ReceiverMsg.send_ack.ordinal()] = p -> {
			System.out.println("Checking Data of received package...is right");
			p = new Package();
			return ReceiverState.WAIT;
		};
		
		trans[ReceiverState.SEND_ACK.ordinal()][ReceiverMsg.send_nack.ordinal()] = p -> {
			System.out.println("Sending true ACK.");
			p = new Package();
			return ReceiverState.WAIT;
		};
		
		trans[ReceiverState.SEND_ACK.ordinal()][ReceiverMsg.send_fin.ordinal()] = p -> {
			System.out.println("Checking Data of received package...is wrong.");
			p = new Package();
			return ReceiverState.SEND_ACK;
		};
		
		trans[ReceiverState.WAIT.ordinal()][ReceiverMsg.timeout_after_first_package.ordinal()] = p -> {
			System.out.println("Sending false ACK.");
			p = new Package();
			return ReceiverState.WAIT;
		};
		
	}
	
	public FileReceiver() {
		try {
			sock = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	
	@FunctionalInterface
	private interface Transition {
		ReceiverState execute(Package p);
	}
	
	private void waitIncoming() throws IOException {
		boolean noPack = true;
		while(noPack) {
			byte[] buffer = new byte[1400];
			DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
			
			try {
				sock.receive(incomingPacket);
				sock.setSoTimeout(1000);
				ip = incomingPacket.getAddress();
				Package pak = new Package(incomingPacket);
				seq = pak.getSeqNum();
				fin = pak.getFin();
				long check = pak.getCheckSum();
				pak.setChecksum();
				if (pak.getSeqNum() == seq) {
					System.out.println("Seq in Ordnung");
				}
				if (check != pak.getCheckSum()) {
					System.out.println("Checksum falsch");
					sendAnswerPacket(backupDataPacket);
				}
				else {
					noPack = false;
					System.out.println("Checksum passt");
				}
				System.out.println("Package erhalten");
			}
			catch (SocketTimeoutException s) {
				System.out.println("Timeout");
				sendAnswerPacket(backupDataPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void sendAnswerPacket(Package pak) throws IOException {
		DatagramPacket dpak = pak.PackageToDatagramPacket();
		sendAnswerPacket(dpak);
	}
	
	private void sendAnswerPacket(DatagramPacket dpak) throws IOException {
		dpak.setAddress(ip);
		dpak.setPort(port);
		sock.send(dpak);
		backupDataPacket = dpak;
	}
	
	private void setupPackage() {
		byte[] empty = new byte[0];
		Package pak = new Package(filename, seq, true, fin, empty);
		sendAnswerPacket(pak);
		if (seq == 0) {
			seq = 1;
		}
		else {
			seq = 0;
		}
	}
	
	public static void main(String[] args) throws SocketException {
		FileReceiver fr = new FileReceiver();
		while(fin) {
			try {
				fr.waitIncoming();
				fr.setupPackage();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Ende");
	}
		
}
