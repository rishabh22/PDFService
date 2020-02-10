package nic.oad.pdfservice;

import nic.oad.pdfservice.commons.PdfSecurity;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;

@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@SpringBootTest
@AutoConfigureMockMvc
public class PdfserviceApplicationTests {

//    private String outputFile = "/Volumes/MACINTOSH H/Users/rishabh/Desktop/mergedPdf.pdf";
    private String outputFile = "D:\\report.pdf";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void contextLoads() throws Exception {
        report();
//        imageToPdf();
        //splitPdf();
        //esignDoc();
//        mergePdfs();
    }


    private void esignDoc() throws Exception {
        MultiValueMap map = new LinkedMultiValueMap();
        map.add("htmlArray[]", Base64Utils.encodeToString("test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>".getBytes()));
        map.add("htmlArray[]", Base64Utils.encodeToString("test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/><br/>test".getBytes()));
        map.add("processId", PdfSecurity.encrypt("421", "DiGnIcPrOcEsSiD!"));
        map.add("signatureCount", "3");
        map.add("remarksRequiredForAll", "SHORT");
        map.add("signAlignment", "RTL");
        map.add("showHeaderFooter", "true");
        map.add("showHeaderOffice", "true");
        MvcResult mvcResult = this.mockMvc.perform(MockMvcRequestBuilders
                //.post("/reportGenerator").params(map))
                .post("/generatePdfForEsign").params(map))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        File file = new File(outputFile);
        FileUtils.writeByteArrayToFile(file, mvcResult.getResponse().getContentAsByteArray());
        System.out.println("test");
    }

    @Test
    public void report() throws Exception {
        MultiValueMap map = new LinkedMultiValueMap();
        //map.add("htmlSource", "\n" +
        //        "lkjka”khd`r isa”ku “kq# dju dh rkfj[kBlahइलेक्ट्रॉनिक्स और सूचना प्रौद्योगिकी मंत्रालयtest<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>test<br/><br/>");


        map.add("htmlSource", "Ministryइलेक्ट्रॉनिक्स");

        map.add("processId", PdfSecurity.encrypt("421", "DiGnIcPrOcEsSiD!"));
        map.add("confidential", "true");
        MvcResult mvcResult = this.mockMvc.perform(MockMvcRequestBuilders
                //.post("/reportGenerator").params(map))
                .post("/reportGeneratorWithHindi").characterEncoding("UTF-8").params(map))
//                .post("/reportGenerator").params(map))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        File file = new File(outputFile);
        FileUtils.writeByteArrayToFile(file, mvcResult.getResponse().getContentAsByteArray());
        System.out.println("test");
    }


    private void mergePdfs() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile("pdfs[]", FileUtils.openInputStream(new File("/Volumes/MACINTOSH H/Users/rishabh/Desktop/test.pdf")));
        MvcResult mvcResult = this.mockMvc.perform(
                MockMvcRequestBuilders
                        .multipart("/mergePdfs")
                        .file(mockMultipartFile)
                        .file(mockMultipartFile)
                        .file(mockMultipartFile)
                        .file(mockMultipartFile)
                        .file(mockMultipartFile)
                        .file(mockMultipartFile)
                        .file(mockMultipartFile)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        File file = new File(outputFile);
        FileUtils.writeByteArrayToFile(file, mvcResult.getResponse().getContentAsByteArray());
        System.out.println("test");
    }

    private void splitPdf() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile("pdf", FileUtils.openInputStream(new File("C:\\Users\\soodr\\OneDrive\\Desktop\\GreensheetWPassport.pdf")));
        MvcResult mvcResult = this.mockMvc.perform(
                MockMvcRequestBuilders
                        .multipart("/splitPdf")
                        .file(mockMultipartFile)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        File file = new File(outputFile);
        FileUtils.writeByteArrayToFile(file, mvcResult.getResponse().getContentAsByteArray());
        System.out.println("test");
    }


    private void imageToPdf() throws Exception {
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("images[]", FileUtils.openInputStream(new File("F:\\DSC_2986.JPG")));
        MockMultipartFile mockMultipartFile2 = new MockMultipartFile("images[]", FileUtils.openInputStream(new File("F:\\DSC05143_sm.jpg")));
        MvcResult mvcResult = this.mockMvc.perform(
                MockMvcRequestBuilders
                        .multipart("/imageToPdf")
                        .file(mockMultipartFile1)
                        .file(mockMultipartFile1)
                        .file(mockMultipartFile1)
                        .file(mockMultipartFile1)
                        .file(mockMultipartFile2)
                        .file(mockMultipartFile2)
                        .file(mockMultipartFile2)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        File file = new File(outputFile);
        FileUtils.writeByteArrayToFile(file, mvcResult.getResponse().getContentAsByteArray());
        System.out.println("test");
    }
}
