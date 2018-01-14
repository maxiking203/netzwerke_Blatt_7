package Package;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.util.zip.CRC32;

public class Package implements Serializable {
	
	private long checkSum;
	private String filename;
	private int seqNum;
	private boolean ack;
	private boolean fin;
	private byte[] content;
	
	public Package() {
		
	}
	
	public Package(String filename, int seqNum, boolean ack, boolean fin, byte[] content) {
		if (seqNum == 0 || seqNum == 1) {
			this.filename = filename;
			this.seqNum = seqNum;
			this.ack = ack;
			this.fin = fin;
			this.content = content;
			
			generateCheckSum();
		}
		else {
			System.out.println("Die Sequenznummer muss 0 oder 1 sein");
		}
	}
	
	public Package(DatagramPacket dpak) {
		Package inpak = udpPackageToPackage(dpak);
		this.filename = inpak.filename;
		this.seqNum = inpak.seqNum;
		this.ack = inpak.ack;
		this.fin = inpak.fin;
		this.content = inpak.content;
		this.checkSum = inpak.checkSum;
	}
	
	
	
	//Generiert eine cheksumme für ein Package, 
	//die Checksum selber is dabei in dem betrachteten byte Array ausgeschlossen
	private void generateCheckSum() {
		CRC32 checkSumObj = new CRC32();
		byte[] packageAsArray = packageToByteArray();
		checkSumObj.update(packageAsArray, 8, packageAsArray.length -8);
		this.checkSum = checkSumObj.getValue();
	}
	
	public long generateCheckSumArray(byte[] input) {
		CRC32 checkSumObj = new CRC32();
		checkSumObj.update(input, 0, input.length);
		return checkSumObj.getValue();
	}
	
	
	///////////////////////////////
	//Umwandlungs Methoden
	///////////////////////////////
	
	
	//Methode um ein erhaltenes DatagramPacket zurück in unser Packageformat umzuwandeln
	public Package udpPackageToPackage(DatagramPacket inPackage) {
		byte[] wholePackage = inPackage.getData();
		
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(wholePackage);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            if (bis != null) {
                try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("ByteArrayInputStream war schon geschlossen, deswegen nicht mehr möglich.");

				}
            }
            if (ois != null) {
                try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("ObjectInputStream war schon geschlossen, deswegen nicht mehr möglich.");
				}
            }
        }
        return (Package) obj;
	}
	
	//Methode um ein erstelltesPackage in ein Byte Array umzuwandeln
	private byte[] packageToByteArray() {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            bytes = bos.toByteArray();
        } catch (IOException e) {
			e.printStackTrace();
			System.out.println("Fehler bei der Umwandlung in ein Byte Array.");
			
		} finally {
            if (oos != null) {
                try {
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("ObjectOutputStream war schon geschlossen, deswegen nicht mehr möglich.");
				}
            }
            if (bos != null) {
                try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("ByteArrayOutputStream war schon geschlossen, deswegen nicht mehr möglich.");
				}
            }
        }
        return bytes;
	}
	
	public DatagramPacket PackageToDatagramPacket() {
		byte [] data = this.packageToByteArray();
		return new DatagramPacket(data, data.length);
	}
	
	
	///////////////////////////////
	//Getter und Setter für Objekt Variablen
	///////////////////////////////
	
	public void setFilename(String name) {
		this.filename = name;
	}
	
	
	public String getFilename() {
		return filename;
	}
	
	
	public void setChecksum() {
		generateCheckSum();
	}
	
	
	public long getCheckSum() {
		return checkSum;
	}
	
	
	public void setSeqNum(int num) {
		this.seqNum = num;
		if (num == 0 || num == 1) {
			this.seqNum = num;
		}
		else {
			System.out.println("Die Sequenznummer muss 0 oder 1 sein, daher wurde sie nicht neu gesetzt.");
		}
	}
	
	
	public int getSeqNum() {
		return seqNum;
	}
	
	
	public boolean getAck() {
		return ack;
	}
	
	
	public void setAck(boolean newAck) {
		this.ack = newAck;
	}
	
	
	public boolean getFin() {
		return fin;
	}
	
	
	public void setfin(boolean newFin) {
		this.fin = newFin;
	}
	
	
	public byte[] getContent() {
		return content;
	}
	
	
	public void setContent(byte[] newContent) {
		this.content = newContent;
	}
	
}
