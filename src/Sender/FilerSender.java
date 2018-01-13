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
	
	private void waitForIncomingPacket() {
		boolean gotpackage = false;
		while(!gotpackage) {
			
			byte[] buffer = new byte[1400];
			DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
			
			try {
				sock.receive(incomingPacket);
				gotpackage = true;
				Package pak = new Package(incomingPacket);
				long check = pak.getCheckSum();
				pak.setChecksum();
//				if (pak.getSeqNum() == seq) {
//					System.out.println("Seq in Ordnung");
//					seq = 1;
//				}
				if (check != pak.getCheckSum()) {
					//resend backupPacket
					System.out.println("Checksum falsch");
				}
				else {
					//proceed
					System.out.println("Checksum passt");
				}
			}
			catch (SocketTimeoutException s) {
				System.out.println("Timeout");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
			System.arraycopy(toSendFile, positionArray, splittedArray, positionArray, 1000);
			positionArray += toSendFile.length - positionArray;	
		}
		return splittedArray;
	}
	
	private void setupPackage() throws IOException {
		byte[] send = splitSendArray();
		if (positionArray >= toSendFile.length) {
			Package pack = new Package(file.getName(), positionArray, true, true, send);
			sendPacket(pack);
		}
		else {
			Package pack = new Package(file.getName(), positionArray, true, false, send);
			sendPacket(pack);
		}

	}
	
	public static void main(String[] args) {
		try {
			new FilerSender(args[0], InetAddress.getByName(args[1]));
		} catch (ArrayIndexOutOfBoundsException e) {
			try {
				FilerSender fs = new FilerSender("default.txt", InetAddress.getByName("127.0.0.1"));
				while(true) {
					fs.setupPackage();
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

}
