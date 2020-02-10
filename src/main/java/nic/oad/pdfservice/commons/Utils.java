package nic.oad.pdfservice.commons;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

import java.io.*;
import java.util.zip.ZipOutputStream;

public class Utils {
    public static void safeClose(InputStream inputStream){
        try{
            if(inputStream!=null){
                inputStream.close();
            }
        } catch (IOException ignored){

        }
    }

    public static void safeClose(OutputStream outputStream){
        try{
            if(outputStream!=null){
                outputStream.close();
            }
        } catch (IOException ignored){

        }
    }

    public static void safeClose(InputStreamReader inputStreamReader){
        try{
            if(inputStreamReader!=null){
                inputStreamReader.close();
            }
        } catch (IOException ignored){

        }
    }

    public static void safeClose(BufferedReader bufferedReader){
        try{
            if(bufferedReader!=null){
                bufferedReader.close();
            }
        } catch (IOException ignored){

        }
    }

    public static void safeClose(PdfWriter pdfWriter){
        try{
            if(pdfWriter!=null){
                pdfWriter.close();
            }
        } catch (Exception ignored){

        }
    }

    public static void safeClose(PdfDocument pdfDocument){
        try{
            if(pdfDocument!=null){
                pdfDocument.close();
            }
        } catch (Exception ignored){

        }
    }

    public static void safeClose(ZipOutputStream zipOutputStream){
        try{
            if(zipOutputStream!=null){
                zipOutputStream.close();
            }
        } catch (Exception ignored){

        }
    }

    public static void safeClose(PdfReader pdfReader){
        try{
            if(pdfReader!=null){
                pdfReader.close();
            }
        } catch (Exception ignored){

        }
    }
}
