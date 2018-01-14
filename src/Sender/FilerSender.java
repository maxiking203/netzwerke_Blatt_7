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
	private DatagramPacket backupPacket;
	private byte[] toSendFile;
	private int seq = 0;
	private int positionArray;
	private SenderState currentState;
	private Transition[][] trans = new Transition[SenderState.values().length][SenderMsg.values().length];
	
	{
		
		trans[SenderState.START.ordinal()][SenderMsg.set_up.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return SenderState.UPDATE_DATA;
		};
		
		trans[SenderState.UPDATE_DATA.ordinal()][SenderMsg.next_package.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return SenderState.SEND_PACKAGE;
		};
		
		trans[SenderState.SEND_PACKAGE.ordinal()][SenderMsg.wait_ack.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return SenderState.WAIT_FOR_ACK;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.ack_true.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return SenderState.UPDATE_DATA;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.ack_false.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			return SenderState.SEND_PACKAGE;
		};
		
		trans[SenderState.WAIT_FOR_ACK.ordinal()][SenderMsg.done.ordinal()] = p -> {
			System.out.println("Sending initial package.");
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
			sock = new DatagramSocket();
			sock.setSoTimeout(500);
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
		dpak.setPort(port);
		sock.send(dpak);
		backupPacket = dpak;
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
					}
					if (check != pak.getCheckSum()) {
						System.out.println("Checksum falsch");
						sendPacket(backupPacket);
					}
					else {
						gotpackage = true;
						System.out.println("Checksum passt");
					}
				}
				catch (SocketTimeoutException s) {
					System.out.println("Timeout");
					sendPacket(backupPacket);
					System.out.println("Resend Backup-Packet");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}
	
	private void update() throws IOException {
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
					fs.update();
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
