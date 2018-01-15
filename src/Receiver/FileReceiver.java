package Receiver;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
	private final int port = 5001;
	private final int sport = 5000;
	private InetAddress ip;
	private int seq = 0;
	private byte[] filearray;
	private int positionArray = 0;
	private static boolean fin = false;
	private DatagramPacket backupDataPacket;
	private Package backupPacket;
	private ReceiverState currentState;
	private Transition[][] trans = new Transition[ReceiverState.values().length][ReceiverMsg.values().length];
	
//	{
//		trans[ReceiverState.WAIT.ordinal()][ReceiverMsg.received.ordinal()] = p -> {
//			System.out.println("Sending first package.");
//			p = new Package();
//			return ReceiverState.SEND_ACK;
//		};
//		
//		trans[ReceiverState.SEND_ACK.ordinal()][ReceiverMsg.send_ack.ordinal()] = p -> {
//			System.out.println("Checking Data of received package...is right");
//			p = new Package();
//			return ReceiverState.WAIT;
//		};
//		
//		trans[ReceiverState.SEND_ACK.ordinal()][ReceiverMsg.send_nack.ordinal()] = p -> {
//			System.out.println("Sending true ACK.");
//			p = new Package();
//			return ReceiverState.WAIT;
//		};
//		
//		trans[ReceiverState.SEND_ACK.ordinal()][ReceiverMsg.send_fin.ordinal()] = p -> {
//			System.out.println("Checking Data of received package...is wrong.");
//			p = new Package();
//			return ReceiverState.SEND_ACK;
//		};
//		
//		trans[ReceiverState.WAIT.ordinal()][ReceiverMsg.timeout_after_first_package.ordinal()] = p -> {
//			System.out.println("Sending false ACK.");
//			p = new Package();
//			return ReceiverState.WAIT;
//		};
//		
//	}
	
	public FileReceiver() {
		this.filearray = new byte[1000];
		try {
			sock = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
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
				fin = pak.getFin();
				filename = pak.getFilename();
				System.out.println(pak.getFilename() + "," + pak.getSeqNum() + "," + pak.getAck() + "," + pak.getFin() + "," + pak.getCheckSum());
				Package test = new Package(pak.getFilename(), pak.getSeqNum(), pak.getAck(), pak.getFin(), pak.getContent());
				long check = test.getCheckSum();
				System.out.println(test.getCheckSum());
				if (pak.getSeqNum() == seq) {
					System.out.println("Seq in Ordnung");
					if (check == pak.getCheckSum()) {
						System.out.println("Checksum passt");
						System.out.println("Package erhalten");
						noPack = false;
						bytesToArray(pak.getContent());
						if (fin) {
							byteArrayToFile(filearray);
						}
						seq = pak.getSeqNum();
						setupPackage(true);
					}
					else {
						System.out.println("Checksum falsch");
						sendAnswerPacket(backupDataPacket);
					}
				}
				else {
					System.out.println("Seq falsch");
					sendAnswerPacket(backupDataPacket);
				}
			}
			catch (SocketTimeoutException s) {
				System.out.println("Timeout");
				sendAnswerPacket(backupDataPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private int randomWithRange(int min, int max) {
	   int range = (max - min) + 1;     
	   return (int)(Math.random() * range) + min;
	}
	
	private void waitIncomingCorrupted(int bit, int thro, int dup) throws IOException {
		boolean noPack = true;
		while(noPack) {
			byte[] buffer = new byte[1400];
			DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
			
			try {
				if (randomWithRange(0,100) > thro) {
					sock.receive(incomingPacket);
				}
				sock.setSoTimeout(1000);
				ip = incomingPacket.getAddress();
				Package pak = new Package(incomingPacket);
				fin = pak.getFin();
				filename = pak.getFilename();
				System.out.println(pak.getFilename() + "," + pak.getSeqNum() + "," + pak.getAck() + "," + pak.getFin() + "," + pak.getCheckSum());
				long check = pak.getCheckSum();
				//pak.setChecksum();
				System.out.println(pak.getCheckSum());
				if (pak.getSeqNum() == seq) {
					System.out.println("Seq in Ordnung");
					if (check == pak.getCheckSum() && randomWithRange(0,100) > bit) {
						System.out.println("Checksum passt");
						System.out.println("Package erhalten");
						noPack = false;
						bytesToArray(pak.getContent());
						if (fin) {
							byteArrayToFile(filearray);
						}
						seq = pak.getSeqNum();
						setupPackage(true);
					}
					else {
						System.out.println("Checksum falsch");
						sendAnswerPacket(backupDataPacket);
					}
				}
				else {
					System.out.println("Seq falsch");
					sendAnswerPacket(backupDataPacket);
				}
			}
			catch (SocketTimeoutException s) {
				System.out.println("Timeout");
				sendAnswerPacket(backupDataPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void bytesToArray(byte[] content) {
			try {
				for (int i = 0; i < content.length; i++) {
					filearray[positionArray] = content[i];
					positionArray++;
				}
			}
			catch (ArrayIndexOutOfBoundsException a) {
				byte[] filearraynew = new byte[(filearray.length + content.length)];
				System.arraycopy(filearray, 0, filearraynew, 0, filearray.length);
				filearray = filearraynew;
				bytesToArray(content);
			}
	}
	
	private void sendAnswerPacket(Package pak) throws IOException {
		DatagramPacket dpak = pak.PackageToDatagramPacket();
		backupPacket = pak;
		sendAnswerPacket(dpak);
	}
	
	private void sendAnswerPacket(DatagramPacket dpak) throws IOException {
		dpak.setAddress(ip);
		dpak.setPort(sport);
		sock.send(dpak);
		backupDataPacket = dpak;
	}
	
	private void setupPackage(boolean ack) throws IOException {
		byte[] empty = new byte[0];
		Package pak = new Package(filename, seq, ack, fin, empty);
		sendAnswerPacket(pak);
		if (ack) {
			if (seq == 0) {
				seq = 1;
			}
			else {
				seq = 0;
			}
		}
	}
	
	private void byteArrayToFile(byte[] content) throws IOException {
		FileOutputStream stream;
		try {
			stream = new FileOutputStream("receive " + filename);
		    stream.write(content);
		    stream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws SocketException {
		FileReceiver fr = new FileReceiver();
		while(!fin) {
			try {
				fr.waitIncoming();
				//fr.waitIncomingCorrupted(10, 5, 5);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Ende");
	}
	
	@FunctionalInterface
	private interface Transition {
		
		ReceiverState execute(ReceiverMsg msg);
		
	}
	
	public void processMsg(ReceiverMsg input){
		System.out.println("INFO Received "+input+" in state "+currentState);
		Transition t = trans[currentState.ordinal()][input.ordinal()];
		if(trans != null){
			currentState = t.execute(input);
		}
		System.out.println("INFO State: "+currentState);
	}
		
}
