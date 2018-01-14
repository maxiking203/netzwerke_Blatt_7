package Sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;

import Package.Package;

public class FilerSender {
	
	private File file;
	private InetAddress ip;
	private FileInputStream fis;
	private DatagramSocket sock;
	private final int port = 5000;
	private final int sport = 5001;
	private DatagramPacket backupDataPacket;
	private byte[] toSendFile;
	private int seq = 0;
	private int positionArray;
	private SenderState currentState;
	private boolean status = true;
	private Package backupPacket;
	private Transition[][] trans = new Transition[SenderState.values().length][SenderMsg.values().length];
	
	{
		
		trans[SenderState.START.ordinal()][SenderMsg.set_up_first.ordinal()] = p -> {
			System.out.println("Setting up first Data.");
			p = new Package();
			return SenderState.SEND;
		};
		
		trans[SenderState.SEND.ordinal()][SenderMsg.wait_ack.ordinal()] = p -> {
			System.out.println("Sending Package.");
			p = new Package();
			return SenderState.WAIT_FOR_ACK;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.ack_true.ordinal()] = p -> {
			System.out.println("Waiting for ACK.");
			p = new Package();
			return SenderState.SEND;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.ack_false.ordinal()] = p -> {
			System.out.println("Got true ack. Setting up new Data");
			p = new Package();
			return SenderState.SEND;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.received_fin.ordinal()] = p -> {
			System.out.println("Got false ACK. Sending Package again.");
			p = new Package();
			return SenderState.END;
		};
		
		
	}
	
	public FilerSender(String filename, InetAddress ip) {
		this.file = new File(filename);
		this.ip = ip;
	
		try {
			this.fis = new FileInputStream(file);
			this.toSendFile = fileToByteArray();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			sock = new DatagramSocket(port);
			sock.setSoTimeout(1000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	private void sendPacket(Package pak) throws IOException {
		DatagramPacket dpak = pak.PackageToDatagramPacket();
		sendPacket(dpak);
	}
	
	private void sendPacket(DatagramPacket dpak) throws IOException {
		dpak.setAddress(ip);
		dpak.setPort(sport);
		sock.send(dpak);
		backupDataPacket = dpak;
	}
	
	private void waitForIncomingPacket() throws IOException {
		boolean gotpackage = false;
		
			
			byte[] buffer = new byte[1400];
			DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
			
			while(!gotpackage) {
				try {
					sock.receive(incomingPacket);
					Package pak = new Package(incomingPacket);
					long check = pak.getCheckSum();
					pak.setChecksum();
					if (pak.getSeqNum() == seq) {
						System.out.println("Seq in Ordnung");
						gotpackage = true;
					}
					else {
						System.out.println("Seq falsch");
						sendPacket(backupDataPacket);
					}
					if (check != pak.getCheckSum()) {
						System.out.println("Checksum falsch");
						sendPacket(backupDataPacket);
					}
					else {
						gotpackage = true;
						System.out.println("Checksum passt");
					}
				}
				catch (SocketTimeoutException s) {
					System.out.println("Timeout");
					sendPacket(backupDataPacket);
					System.out.println("Resend Backup-Packet");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}
	
	private void prepare() throws IOException {
		if (seq == 0) {
			seq = 1;
		}
		else {
			seq = 0;
		}
		
		Package pack = setupPackage();
		sendPacket(pack);
	}
	
	private byte[] fileToByteArray() {
		byte[] data = null;
        try {
            fis = new FileInputStream(file);
            data = new byte[(int) file.length()];
            fis.read(data);
            
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally {
            try {
                if(fis != null) 
                	fis.close();
            } 
            catch (IOException e) {}
        }
        return data; 
	}
	
	private byte[] splitSendArray() {
		byte[] splittedArray;
		if (toSendFile.length - positionArray >= 1000) {
			splittedArray = new byte[1000];
			System.arraycopy(toSendFile, positionArray, splittedArray, positionArray, 1000);
			positionArray += 1000;	
		}
		else {
			splittedArray = new byte[toSendFile.length - positionArray];
			System.arraycopy(toSendFile, positionArray, splittedArray, positionArray, toSendFile.length - positionArray);
			positionArray += toSendFile.length - positionArray;	
		}
		return splittedArray;
	}
	
	private Package setupPackage() throws IOException {
		byte[] send = splitSendArray();
		if (positionArray >= toSendFile.length) {
			Package pack = new Package(file.getName(), seq, true, true, send);
			return pack;
		}
		else {
			Package pack = new Package(file.getName(), seq, true, false, send);
			return pack;
		}
	}
	
	public static void main(String[] args) {
		try {
			new FilerSender(args[0], InetAddress.getByName(args[1]));
		} catch (ArrayIndexOutOfBoundsException e) {
			try {
				FilerSender fs = new FilerSender("default.txt", InetAddress.getByName("127.0.0.1"));
				while(true) {
					fs.prepare();
					fs.waitForIncomingPacket();
				}
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FunctionalInterface
	private interface Transition {
		
		SenderState execute(Package p);
		
	}

}
