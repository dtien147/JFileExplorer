import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

public class Explorer {
    private final FileSystemView fsv = FileSystemView.getFileSystemView(); 
    private File copyClipboard;
    private File cutClipboard;
    
    public Object[] getFolderInfo(File folder) {
        ImageIcon folderIcon = new ImageIcon("folder.png");        
        Object[] data = {folderIcon, folder.getName(), 
            fsv.getSystemTypeDescription(folder), " ", folder};        
        return data;
    }
    
    private String getSize(File file) {
        double size = file.length();        
        if(size >= 1024) {
            size /= 1024;
            if(size >= 1024) {
                size /= 1024;
                if(size >= 1024) {
                    size /= 1024;
                    return String.format("%.2f", size) + " GB";
                }
                
                return String.format("%.2f", size) + " MB";
            }
            
            return String.format("%.2f", size) + " KB";
        }
        
        return String.format("%.2f", size) + " Byte";
    }
    
    public Object[] getFileInfo(File file) {
        ImageIcon fileIcon = new ImageIcon("file.png");
        Object[] data = {fileIcon, file.getName(), 
            fsv.getSystemTypeDescription(file), getSize(file), file};
        return data;
    }
    
    public Object[] getFolderContent(File folder) {
        Object[] data = new Object[folder.listFiles().length];
        Object[] files = new Object[folder.listFiles().length];
        Object[] folders = new Object[folder.listFiles().length];
        int total = folder.listFiles().length;        
        int numFile = 0;
        int numFolder = 0;
        
        for(File content: folder.listFiles()) {
            if(content.isFile()) {
                files[numFile++] = getFileInfo(content);
            }
            else {
                folders[numFolder++] = getFolderInfo(content);
            }            
        }
        
        for(int i = 0; i < numFolder; i++) {
            data[i] = folders[i];
        }
        for(int i = numFolder; i < total; i++) {
            data[i] = files[i - numFolder];
        }
        
        return data;
    }
    
    public Object[] getDriveInfo(File drive) {        
        if(fsv.getSystemTypeDescription(drive).equals("Local Disk")) {
            ImageIcon driveIcon = new ImageIcon("drive.png");        
            Object[] data = {driveIcon, fsv.getSystemDisplayName(drive),
                fsv.getSystemTypeDescription(drive), " "};        
            return data;
        }
             
        return null;
    }
    
    public void openText(File file) {        
        if(fsv.getSystemTypeDescription(file).equals("Text Document")) {
            TextEditor editor = new TextEditor(file);
            editor.setModal(true);
            editor.show();
        } 
        else {
            JOptionPane.showMessageDialog(null, "Not supported file type", 
                    "Not supported", JOptionPane.INFORMATION_MESSAGE);
        }
    }        
    
    public void copyTo(File src, File dest) {        
        try {
            if(src.isDirectory()) {               
                while(dest.exists()) {
                    dest = new File(dest.getAbsolutePath() + " - Copy");
                }
                
                dest.mkdir();
    		String files[] = src.list();    		
    		for (String file : files) {
    		   File srcFile = new File(src, file);
    		   File destFile = new File(dest, file);
    		   copyTo(srcFile,destFile);
    		}
            }
            else {
                InputStream in = new FileInputStream(src);
                OutputStream out = new FileOutputStream(dest);
                byte[] buffer = new byte[1024];
                int length;

                while ((length = in.read(buffer)) > 0){
                   out.write(buffer, 0, length);
                }

                in.close();
                out.close();
            }
        } catch(Exception e) {

        }        
    }
    
    public void cutTo(File src, File dest) {
        try {
            while(dest.exists()) {
                dest = new File(dest.getAbsolutePath() + " - Cut");
            }
            src.renameTo(dest);
        } catch(Exception e) {

        }        
    }
    
    public void delete(File src) {
        if(src.isDirectory()) {
            File[] files = src.listFiles();
            for (File file : files) {
               delete(file);
            }            
        }
        src.delete();
    }
    
    public void rename(File file) {
        Rename rename = new Rename(file);
        rename.setModal(true);
        rename.show();
    }
    
    private void compressFile(ZipOutputStream zos, File file, String parent) {
        byte[] buffer = new byte[1024];    	
    	try {    		                        
            ZipEntry ze= new ZipEntry(parent + file.getName());
            zos.putNextEntry(ze);
            FileInputStream in = new FileInputStream(file);

            int len;
            while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
            }

            in.close();
            zos.closeEntry();
    	} catch(IOException ex){}
    }
    
    private void compressFolder(ZipOutputStream zos, File folder, String parent) {
        String parentPath = parent + folder.getName() + File.separator;
        for(File object: folder.listFiles()){
            if(object.isDirectory())
                compressFolder(zos, object, parentPath);
            else
                compressFile(zos, object, parentPath);
        }
    }
    
    public void compress(File src) {
        try {
            FileOutputStream fos = new FileOutputStream(src.getAbsolutePath() 
                    + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fos);
            if(src.isDirectory()) {
                compressFolder(zos, src, "");
            }                
            else { 
                compressFile(zos, src, "");
            }            
            zos.close();
            fos.close();
        } catch(Exception e) {}
    }           
    
    public void extractFile(ZipInputStream zis, File file) {
        byte[] buffer = new byte[1024];    	
        try {            
            FileOutputStream fos = new FileOutputStream(file);             
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();   
        } catch (Exception ex) {
            
        }
    }
    
    public void extract(File zipFile) {
        if(isValid(zipFile)) {
            try {                
                String parentPath = zipFile.getParent();
                File folder = new File(parentPath);
                if(!folder.exists()){
                    folder.mkdir();
                }

                ZipInputStream zis = 
                        new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry ze = zis.getNextEntry();                

                while(ze!=null){
                    String fileName = ze.getName();
                    File object = new File(parentPath + File.separator + fileName);
                    if(ze.isDirectory()) 
                        object.mkdir();
                    else 
                        extractFile(zis, object);
                    
                    ze = zis.getNextEntry();
                }

                zis.closeEntry();                
                zis.close();
            }catch(IOException ex){}
        }
    }
    
    private boolean isValid(File zipFile) {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(zipFile);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                    zipfile = null;
                }
            } catch (IOException e) {
            }
        }
    }
    
    public void split(File file) {
        if(! file.isDirectory()) {
            Split split = new Split(file);
            split.setModal(true);
            split.show();
        }
    }
    
    private ArrayList<File> sortParts(ArrayList<File> parts) {
        for(int i = 0; i < parts.size() - 1; i++) {
            for(int j = i + 1; j < parts.size(); j++) {
                String aName = parts.get(i).getName();
                String bName = parts.get(j).getName();
                if(aName.compareTo(bName) > 0) {
                    File temp = parts.get(i);
                    parts.set(i, parts.get(j));
                    parts.set(j, temp);
                }
            }
        }
        
        return parts;
    }
    
    private ArrayList<File> findParts(File firstFile) {
        ArrayList<File> parts = new ArrayList();
        File folder = firstFile.getParentFile();
        String baseName = firstFile.getName().substring(0, 
                    firstFile.getName().lastIndexOf("."));
        
        for(File file: folder.listFiles()) {        
            if(file.getName().contains(baseName)) {
                String fileName = file.getName().substring(0, 
                        file.getName().lastIndexOf("."));
                if(fileName.equals(baseName)) {
                    parts.add(file);
                }
            }
        }
        
        return sortParts(parts);
    }
    
    public void merge(File firstFile) {
        if(firstFile.isDirectory()) {
            return;
        }
        byte[] buffer = new byte[1024];
        if(firstFile.getName().endsWith("part1")) {
            ArrayList<File> parts = findParts(firstFile);
            String baseName = firstFile.getName().substring(0, 
                    firstFile.getName().lastIndexOf("."));
            try {
                OutputStream out = new FileOutputStream(firstFile.getParent() + 
                        "/" + baseName);
                for(File part: parts) {
                    InputStream in = new FileInputStream(part);
                    int length;
                    while((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                    
                    in.close();
                }              
                
                out.close();
            } catch(Exception e) {}            
        }
        else {
            JOptionPane.showMessageDialog(null, "Please select part one");
        }
    }
}
