package me.rancraftplayz.mappingsconverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class ZipUtils {
    public static void addFilesToZip(File source, File[] files) {
        try
        {

            File tmpZip = File.createTempFile(source.getName(), null);
            tmpZip.delete();
            if(!source.renameTo(tmpZip))
            {
                throw new Exception("Could not make temp file (" + source.getName() + ")");
            }
            byte[] buffer = new byte[1024];
            ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(source));

            for(int i = 0; i < files.length; i++)
            {
                InputStream in = new FileInputStream(files[i]);
                out.putNextEntry(new ZipEntry(files[i].getName()));
                for(int read = in.read(buffer); read > -1; read = in.read(buffer))
                {
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
                in.close();
            }

            for(ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry())
            {
                out.putNextEntry(ze);
                for(int read = zin.read(buffer); read > -1; read = zin.read(buffer))
                {
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
            }

            out.close();
            tmpZip.delete();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
