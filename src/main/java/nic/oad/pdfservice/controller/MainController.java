package nic.oad.pdfservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfSignatureFormField;
import com.itextpdf.forms.fields.PdfTextFormField;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.ILineDrawer;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TabAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.renderer.CellRenderer;
import com.itextpdf.layout.renderer.DrawContext;
import nic.oad.pdfservice.commons.Constants;
import nic.oad.pdfservice.commons.PdfSecurity;
import nic.oad.pdfservice.commons.Utils;
import nic.oad.pdfservice.model.domain.EmpMast;
import nic.oad.pdfservice.model.domain.digitalnic.PdfServiceEsignLogs;
import nic.oad.pdfservice.model.domain.digitalnic.PdfServiceLogs;
import nic.oad.pdfservice.model.dto.SignProperty;
import nic.oad.pdfservice.service.ApplicationService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.*;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@SuppressWarnings({"unchecked", "duplicates"})
public class MainController implements ResourceLoaderAware, Constants {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ResourceLoader resourceLoader;

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private HttpServletRequest request;

    private byte[] logoBytes;

    private final SimpleDateFormat dateFormat;

    public MainController() {
        dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa");
    }

    private EmpMast empMast;

    private ApplicationService applicationService;

    /* Constants form itext5 */
    //private static final PdfNumber INVERTEDPORTRAIT = new PdfNumber(180);
    //private static final PdfNumber LANDSCAPE = new PdfNumber(90);
    //private static final PdfNumber PORTRAIT = new PdfNumber(0);
    //private static final PdfNumber SEASCAPE = new PdfNumber(270);

    private void logError(PdfServiceLogs pdfServiceLogs, String errorDesc) {
        pdfServiceLogs.setErrorDesc(errorDesc);
        pdfServiceLogs.setStatus(0);
        applicationService.save(pdfServiceLogs);
    }

    private void logEsignPdfError(PdfServiceEsignLogs pdfServiceEsignLogs, String errorDesc) {
        pdfServiceEsignLogs.setErrorDesc(errorDesc);
        pdfServiceEsignLogs.setStatus(0);
        applicationService.save(pdfServiceEsignLogs);
    }


    private boolean validateOadAuthority(String clientIp) {
        try {
            List ipMasters = applicationService.getIpMastersByIpAddress(clientIp);
            return ipMasters != null && !ipMasters.isEmpty();
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return false;
    }


    @ResponseBody
    @RequestMapping("/reportGenerator")
    public ResponseEntity<byte[]> generateReport(HttpServletRequest request, String theme, String htmlSource,
                                                 String processId, String orientation, String filename,
                                                 Boolean showHeaderFooter, Boolean confidential,
                                                 Boolean showHeaderDigNicLogo, Boolean showHeaderOffice,
                                                 Boolean showFooterGenBy, Boolean showFooterPageNumber, Boolean showFooterIp,
                                                 Integer leftMargin, Integer rightMargin, Integer bottomMargin, Integer topMargin)
            throws IOException {
        //Collections.list(request.getHeaderNames()).forEach(header-> System.out.println("Header: "+ header + " : " +request.getHeader(header)));

        if (htmlSource == null || htmlSource.trim().isEmpty() || processId == null) {
            logger.error("htmlSource or processId null");
            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
        }

        String clientIp = request.getHeader("X-Real-IP");
        if (clientIp == null || clientIp.isEmpty())
            clientIp = request.getRemoteAddr();


        logger.info("Processing request with process id: " + processId + " Client Ip: " + clientIp);
        PdfServiceLogs pdfServiceLogs = new PdfServiceLogs();
        pdfServiceLogs.setClientIp(clientIp);
        pdfServiceLogs.setUserAgent(request.getHeader("User-Agent"));
        pdfServiceLogs.setReferer(request.getHeader("referer"));
        pdfServiceLogs.setHost(request.getRemoteHost());
        pdfServiceLogs.setTheme(theme);
        pdfServiceLogs.setConfidential(confidential);
        if (filename == null || filename.trim().isEmpty()) {
            filename = "Report.pdf";
        }
        pdfServiceLogs.setFileName(filename);
        String empCodeDec = PdfSecurity.decrypt(processId, "DiGnIcPrOcEsSiD!");
        logger.info("Decrypted process id(EmpCode): " + empCodeDec);
        if (empCodeDec.isEmpty()) {
            logError(pdfServiceLogs, "Decrypted EmpCode Empty");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        Long empCode = null;
        try {
            empCode = Long.parseLong(empCodeDec);
        } catch (Exception ignored) {

        }
        if (empCode == null) {
            logError(pdfServiceLogs, "Decrypted EmpCode could not be parsed as long");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        empMast = applicationService.getApmDetails(empCode);
        if (empMast == null) {
            logError(pdfServiceLogs, "APM null for EmpCode");
            return new ResponseEntity<>("Invalid Emp Code".getBytes(), HttpStatus.BAD_REQUEST);
        }
        logger.info("Request Initiator: " + empMast.getEmpName());
        pdfServiceLogs.setEmpCode(empMast);

        //Include Font Awesome css

        StringBuilder cssAdminLte = new StringBuilder();
        if (theme != null && !theme.isEmpty()) {
            if (theme.equalsIgnoreCase("wcar")) {
                cssAdminLte.append(getCssString("static\\css\\gentelella\\custom.css"));
                cssAdminLte.append(getCssString("static\\css\\gentelella\\wcar_style.css"));
            } else if (theme.equalsIgnoreCase("digital") || theme.equalsIgnoreCase("adminlte")) {
                cssAdminLte.append(getCssString("static\\css\\AdminLTE.css"));
                cssAdminLte.append(getCssString("static\\css\\_all-skins.css"));
            }
        }
        //ClassPathResource imgFile = new ClassPathResource("static/images/digitalnic_logo.png");
        ClassPathResource imgFile = new ClassPathResource("static/images/dn1.png");
        try {
            //bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            logoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
        } catch (IOException ignored) {

        }
        String cssBootstrap = getCssString("static\\css\\bootstrap.css") +
                getCssString("static\\font-awesome\\css\\font-awesome.css");
        htmlSource = "<style>" + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + htmlSource.replaceAll("col-md", "col-xs");
        //htmlSource="<style>"+ css.toString()+"div{color:blue!important;}</style>"+htmlSource;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        //String filename = "Report.pdf";
        headers.setContentDispositionFormData("file", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        //ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getHeader("referer")));
        trustAllHosts();
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getRequestURL().toString().replace("http:", "https:")));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(baos);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        pdfDocument.setTagged();
        pdfDocument.setDefaultPageSize(PageSize.A4);

        boolean showHeaderFooterFinal = true;
        logger.info("Request from IP: " + clientIp);
        if (showHeaderFooter != null && validateOadAuthority(clientIp) && !showHeaderFooter)
            showHeaderFooterFinal = false;
        pdfServiceLogs.setShowHeaderFooter(showHeaderFooterFinal);

        PageXofY footerHandler = null;
        if (showHeaderFooterFinal) {
            Header headerHandler = new Header(confidential);

            Date fileTimestamp = new Date();
            pdfServiceLogs.setFileTimestamp(fileTimestamp);
            footerHandler = new PageXofY(/*pdfDocument, */fileTimestamp);

            //Assign event-handlers
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

        }

        /*if(orientation!=null && orientation.equalsIgnoreCase("landscape")) {
            PageOrientationsEventHandler orientationsEventHandler = new PageOrientationsEventHandler();
            orientationsEventHandler.setOrientation(LANDSCAPE);
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, orientationsEventHandler);
        }*/

        //HtmlConverter.convertToPdf(htmlSource, pdfDocument,converterProperties);
        Document document;
        if (orientation != null && orientation.equalsIgnoreCase("landscape")) {
            pdfServiceLogs.setOrientation("L");
            document = new Document(pdfDocument, PageSize.A4.rotate());
        } else {
            pdfServiceLogs.setOrientation("P");
            document = new Document(pdfDocument);
        }
        //PdfFont hindiFont = PdfFontFactory.createFont("/ARIALUNI.TTF");
        //PdfFont hindiFont = PdfFontFactory.createFont(System.getenv("windir")+"\\fonts\\K100.TTF",);
        //PdfFont hindiFont = PdfFontFactory.createFont("/ARIALUNI.TTF");

        /*byte[] hindiFontBytes=null;
        ClassPathResource hindiFontResource = new ClassPathResource("static/fonts/ARIALUNI.TTF");
        try {
            //bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            hindiFontBytes = StreamUtils.copyToByteArray(hindiFontResource.getInputStream());
        } catch (IOException ignored) {

        }


        PdfFont hindiFont = PdfFontFactory.createFont(hindiFontBytes, PdfEncodings.UTF8);
        document.setFont(hindiFont);*/

        if (showHeaderFooterFinal)
            document.setMargins(75, 35, 75, 35);

        else {
            topMargin = (topMargin != null) ? topMargin : 40;
            rightMargin = (rightMargin != null) ? rightMargin : 35;
            bottomMargin = (bottomMargin != null) ? bottomMargin : 25;
            leftMargin = (leftMargin != null) ? leftMargin : 40;
            document.setMargins(topMargin, rightMargin, bottomMargin, leftMargin);
        }


        /*Document document = HtmlConverter.convertToDocument(htmlSource, pdfDocument, converterProperties);*/

        List<IElement> elements;
        try {
            elements = HtmlConverter.convertToElements(htmlSource, converterProperties);
        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("IOException", e);
            logError(pdfServiceLogs, "Exception while converting html to pdf elements: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
        }
        for (IElement element : elements) {
            document.add((IBlockElement) element);
        }

        document.getRenderer().close();
        if (showHeaderFooterFinal)
            footerHandler.writeTotal(pdfDocument);
        //pdfDocument.close();
        //pdfDocument.close();
        document.close();

        //pdfWriter.close();
        //HtmlConverter.convertToPdf(htmlSource, baos, converterProperties);
        //ResponseEntity<byte[]> response = new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        pdfServiceLogs.setStatus(1);
        if (applicationService.save(pdfServiceLogs) == null) {
            logger.error("Could not save log object for request initiated by: " + empMast.getEmpName());
            return new ResponseEntity<>("Could not process your request. Please contact the administrator".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
        //return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
        //return response;
    }


    @ResponseBody
    @RequestMapping("/reportGeneratorWithHindi")
    public ResponseEntity<byte[]> reportGeneratorWithHindi(HttpServletRequest request, String theme, String htmlSource,
                                                           String processId, String orientation, String filename,
                                                           Boolean showHeaderFooter, Boolean confidential,
                                                           Boolean showHeaderDigNicLogo, Boolean showHeaderOffice,
                                                           Boolean showFooterGenBy, Boolean showFooterPageNumber, Boolean showFooterIp,
                                                           Integer leftMargin, Integer rightMargin, Integer bottomMargin, Integer topMargin) throws InterruptedException, IOException{
        //Collections.list(request.getHeaderNames()).forEach(header-> System.out.println("Header: "+ header + " : " +request.getHeader(header)));

        if (htmlSource == null || htmlSource.trim().isEmpty() || processId == null) {
            logger.error("htmlSource or processId null");
            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
        }

        String clientIp = request.getHeader("X-Real-IP");
        if (clientIp == null || clientIp.isEmpty())
            clientIp = request.getRemoteAddr();


        logger.info("Processing request with process id: " + processId + " Client Ip: " + clientIp);
        PdfServiceLogs pdfServiceLogs = new PdfServiceLogs();
        pdfServiceLogs.setClientIp(clientIp);
        pdfServiceLogs.setUserAgent(request.getHeader("User-Agent"));
        pdfServiceLogs.setReferer(request.getHeader("referer"));
        pdfServiceLogs.setHost(request.getRemoteHost());
        pdfServiceLogs.setTheme(theme);
        pdfServiceLogs.setConfidential(confidential);
        if (filename == null || filename.trim().isEmpty()) {
            filename = "Report.pdf";
        }
        pdfServiceLogs.setFileName(filename);
        String empCodeDec = PdfSecurity.decrypt(processId, "DiGnIcPrOcEsSiD!");
        logger.info("Decrypted process id(EmpCode): " + empCodeDec);
        if (empCodeDec.isEmpty()) {
            logError(pdfServiceLogs, "Decrypted EmpCode Empty");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        Long empCode = null;
        try {
            empCode = Long.parseLong(empCodeDec);
        } catch (Exception ignored) {

        }
        if (empCode == null) {
            logError(pdfServiceLogs, "Decrypted EmpCode could not be parsed as long");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        empMast = applicationService.getApmDetails(empCode);
        if (empMast == null) {
            logError(pdfServiceLogs, "APM null for EmpCode");
            return new ResponseEntity<>("Invalid Emp Code".getBytes(), HttpStatus.BAD_REQUEST);
        }
        logger.info("Request Initiator: " + empMast.getEmpName());
        pdfServiceLogs.setEmpCode(empMast);

        //Include Font Awesome css

        StringBuilder cssAdminLte = new StringBuilder();
        if (theme != null && !theme.isEmpty()) {
            if (theme.equalsIgnoreCase("wcar")) {
                cssAdminLte.append(getCssString("static\\css\\gentelella\\custom.css"));
                cssAdminLte.append(getCssString("static\\css\\gentelella\\wcar_style.css"));
            } else if (theme.equalsIgnoreCase("digital") || theme.equalsIgnoreCase("adminlte")) {
                cssAdminLte.append(getCssString("static\\css\\AdminLTE.css"));
                cssAdminLte.append(getCssString("static\\css\\_all-skins.css"));
            }
        }
        //ClassPathResource imgFile = new ClassPathResource("static/images/digitalnic_logo.png");
        ClassPathResource imgFile = new ClassPathResource("static/images/dn1.png");
        try {
            //bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            logoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
        } catch (IOException ignored) {

        }
        String cssBootstrap = getCssString("static\\css\\bootstrap.css") +
                getCssString("static\\font-awesome\\css\\font-awesome.css");
        htmlSource = "<style>" + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + htmlSource.replaceAll("col-md", "col-xs");
        //htmlSource="<style>"+ css.toString()+"div{color:blue!important;}</style>"+htmlSource;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        //String filename = "Report.pdf";
        headers.setContentDispositionFormData("file", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        //ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getHeader("referer")));
        trustAllHosts();
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getRequestURL().toString().replace("http:", "https:")));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Pdf pdf = new Pdf();
        pdf.setAllowMissingAssets();
        pdf.addPageFromString(htmlSource);


        PdfWriter pdfWriter = new PdfWriter(baos);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        pdfDocument.setTagged();
        pdfDocument.setDefaultPageSize(PageSize.A4);

        boolean showHeaderFooterFinal = true;
        logger.info("Request from IP: " + clientIp);
        if (showHeaderFooter != null && validateOadAuthority(clientIp) && !showHeaderFooter)
            showHeaderFooterFinal = false;
        pdfServiceLogs.setShowHeaderFooter(showHeaderFooterFinal);

        PageXofY footerHandler = null;
        if (showHeaderFooterFinal) {
            Header headerHandler = new Header(confidential);

            Date fileTimestamp = new Date();
            pdfServiceLogs.setFileTimestamp(fileTimestamp);
            footerHandler = new PageXofY(/*pdfDocument, */fileTimestamp);

            //Assign event-handlers
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

        }

        /*if(orientation!=null && orientation.equalsIgnoreCase("landscape")) {
            PageOrientationsEventHandler orientationsEventHandler = new PageOrientationsEventHandler();
            orientationsEventHandler.setOrientation(LANDSCAPE);
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, orientationsEventHandler);
        }*/

        //HtmlConverter.convertToPdf(htmlSource, pdfDocument,converterProperties);
        Document document;
        if (orientation != null && orientation.equalsIgnoreCase("landscape")) {
            pdfServiceLogs.setOrientation("L");
            document = new Document(pdfDocument, PageSize.A4.rotate());
        } else {
            pdfServiceLogs.setOrientation("P");
            document = new Document(pdfDocument);
        }
        //PdfFont hindiFont = PdfFontFactory.createFont("/ARIALUNI.TTF");
        //PdfFont hindiFont = PdfFontFactory.createFont(System.getenv("windir")+"\\fonts\\K100.TTF",);
        //PdfFont hindiFont = PdfFontFactory.createFont("/ARIALUNI.TTF");

        /*byte[] hindiFontBytes=null;
        ClassPathResource hindiFontResource = new ClassPathResource("static/fonts/ARIALUNI.TTF");
        try {
            //bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            hindiFontBytes = StreamUtils.copyToByteArray(hindiFontResource.getInputStream());
        } catch (IOException ignored) {

        }


        PdfFont hindiFont = PdfFontFactory.createFont(hindiFontBytes, PdfEncodings.UTF8);
        document.setFont(hindiFont);*/

        if (showHeaderFooterFinal)
            document.setMargins(75, 35, 75, 35);

        else {
            topMargin = (topMargin != null) ? topMargin : 40;
            rightMargin = (rightMargin != null) ? rightMargin : 35;
            bottomMargin = (bottomMargin != null) ? bottomMargin : 25;
            leftMargin = (leftMargin != null) ? leftMargin : 40;
            document.setMargins(topMargin, rightMargin, bottomMargin, leftMargin);
        }


        /*Document document = HtmlConverter.convertToDocument(htmlSource, pdfDocument, converterProperties);*/

        List<IElement> elements;
        try {
            elements = HtmlConverter.convertToElements(htmlSource, converterProperties);
        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("IOException", e);
            logError(pdfServiceLogs, "Exception while converting html to pdf elements: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
        }
        for (IElement element : elements) {
            document.add((IBlockElement) element);
        }

        document.getRenderer().close();
        if (showHeaderFooterFinal)
            footerHandler.writeTotal(pdfDocument);
        //pdfDocument.close();
        //pdfDocument.close();
        document.close();

        //pdfWriter.close();
        //HtmlConverter.convertToPdf(htmlSource, baos, converterProperties);
        //ResponseEntity<byte[]> response = new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        pdfServiceLogs.setStatus(1);
        if (applicationService.save(pdfServiceLogs) == null) {
            logger.error("Could not save log object for request initiated by: " + empMast.getEmpName());
            return new ResponseEntity<>("Could not process your request. Please contact the administrator".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdf.getPDF());
        //return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
        //return response;
    }

    @ResponseBody
    @RequestMapping("/reportGeneratorWithoutHeaderFooter")
    public ResponseEntity<byte[]> generateReportWithoutHeaderFooter(HttpServletRequest request, String theme, String htmlSource,
                                                                    String processId, String orientation, String filename,
                                                                    Boolean showHeaderFooter, Boolean confidential,
                                                                    Integer leftMargin, Integer rightMargin, Integer bottomMargin, Integer topMargin)
            throws IOException {
        //Collections.list(request.getHeaderNames()).forEach(header-> System.out.println("Header: "+ header + " : " +request.getHeader(header)));
        if (htmlSource == null || htmlSource.trim().isEmpty() || processId == null) {
            logger.error("htmlSource or processId null");
            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
        }

        String clientIp = request.getHeader("X-Real-IP");
        if (clientIp == null || clientIp.isEmpty())
            clientIp = request.getRemoteAddr();


        logger.info("Processing request with process id: " + processId + " Client Ip: " + clientIp);
        PdfServiceLogs pdfServiceLogs = new PdfServiceLogs();
        pdfServiceLogs.setClientIp(clientIp);
        pdfServiceLogs.setUserAgent(request.getHeader("User-Agent"));
        pdfServiceLogs.setReferer(request.getHeader("referer"));
        pdfServiceLogs.setHost(request.getRemoteHost());
        pdfServiceLogs.setTheme(theme);
        pdfServiceLogs.setConfidential(confidential);
        if (filename == null || filename.trim().isEmpty()) {
            filename = "Report.pdf";
        }
        pdfServiceLogs.setFileName(filename);
        String empCodeDec = PdfSecurity.decrypt(processId, "DiGnIcPrOcEsSiD!");
        logger.info("Decrypted process id(EmpCode): " + empCodeDec);
        if (empCodeDec.isEmpty()) {
            logError(pdfServiceLogs, "Decrypted EmpCode Empty");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        Long empCode = null;
        try {
            empCode = Long.parseLong(empCodeDec);
        } catch (Exception ignored) {

        }
        if (empCode == null) {
            logError(pdfServiceLogs, "Decrypted EmpCode could not be parsed as long");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        empMast = applicationService.getApmDetails(empCode);
        if (empMast == null) {
            logError(pdfServiceLogs, "APM null for EmpCode");
            return new ResponseEntity<>("Invalid Emp Code".getBytes(), HttpStatus.BAD_REQUEST);
        }
        logger.info("Request Initiator: " + empMast.getEmpName());
        pdfServiceLogs.setEmpCode(empMast);

        //Include Font Awesome css

        StringBuilder cssAdminLte = new StringBuilder();
        if (theme != null && !theme.isEmpty()) {
            if (theme.equalsIgnoreCase("wcar")) {
                cssAdminLte.append(getCssString("static\\css\\gentelella\\custom.css"));
                cssAdminLte.append(getCssString("static\\css\\gentelella\\wcar_style.css"));
            } else if (theme.equalsIgnoreCase("digital") || theme.equalsIgnoreCase("adminlte")) {
                cssAdminLte.append(getCssString("static\\css\\AdminLTE.css"));
                cssAdminLte.append(getCssString("static\\css\\_all-skins.css"));
            }
        }
        //ClassPathResource imgFile = new ClassPathResource("static/images/digitalnic_logo.png");
        ClassPathResource imgFile = new ClassPathResource("static/images/dn1.png");
        try {
            //bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            logoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
        } catch (IOException ignored) {

        }
        String cssBootstrap = getCssString("static\\css\\bootstrap.css") +
                getCssString("static\\font-awesome\\css\\font-awesome.css");
        htmlSource = "<style>" + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + htmlSource.replaceAll("col-md", "col-xs");
        //htmlSource="<style>"+ css.toString()+"div{color:blue!important;}</style>"+htmlSource;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        //String filename = "Report.pdf";
        headers.setContentDispositionFormData("file", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        //ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getHeader("referer")));
        trustAllHosts();
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getRequestURL().toString().replace("http:", "https:")));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(baos);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        pdfDocument.setTagged();
        pdfDocument.setDefaultPageSize(PageSize.A4);

        boolean showHeaderFooterFinal = true;
        if (showHeaderFooter != null && (clientIp.equals("10.0.0.0") || clientIp.startsWith("10.0.0.") || clientIp.startsWith("10.0.0.") || clientIp.equals("164.0.0.0") || clientIp.equals("164.0.0.0") || clientIp.equals("164.0.0.0")) && !showHeaderFooter)
            showHeaderFooterFinal = false;
        showHeaderFooterFinal = false;
        pdfServiceLogs.setShowHeaderFooter(showHeaderFooterFinal);

        PageXofY footerHandler = null;
        if (showHeaderFooterFinal) {
            Header headerHandler = new Header(confidential);

            Date fileTimestamp = new Date();
            pdfServiceLogs.setFileTimestamp(fileTimestamp);
            footerHandler = new PageXofY(/*pdfDocument, */fileTimestamp);

            //Assign event-handlers
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

        }

        /*if(orientation!=null && orientation.equalsIgnoreCase("landscape")) {
            PageOrientationsEventHandler orientationsEventHandler = new PageOrientationsEventHandler();
            orientationsEventHandler.setOrientation(LANDSCAPE);
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, orientationsEventHandler);
        }*/

        //HtmlConverter.convertToPdf(htmlSource, pdfDocument,converterProperties);
        Document document;
        if (orientation != null && orientation.equalsIgnoreCase("landscape")) {
            pdfServiceLogs.setOrientation("L");
            document = new Document(pdfDocument, PageSize.A4.rotate());
        } else {

            pdfServiceLogs.setOrientation("P");
            document = new Document(pdfDocument);
        }
        //PdfFont hindiFont = PdfFontFactory.createFont("/ARIALUNI.TTF");
        //document.setFont(hindiFont);

        if (showHeaderFooterFinal)
            document.setMargins(75, 35, 75, 35);
        else {
            topMargin = (topMargin != null) ? topMargin : 40;
            rightMargin = (rightMargin != null) ? rightMargin : 35;
            bottomMargin = (bottomMargin != null) ? bottomMargin : 25;
            leftMargin = (leftMargin != null) ? leftMargin : 40;
            document.setMargins(topMargin, rightMargin, bottomMargin, leftMargin);
        }


        /*Document document = HtmlConverter.convertToDocument(htmlSource, pdfDocument, converterProperties);*/

        List<IElement> elements;
        try {
            elements = HtmlConverter.convertToElements(htmlSource, converterProperties);
        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("IOException", e);
            logError(pdfServiceLogs, "Exception while converting html to pdf elements: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
        }
        for (IElement element : elements) {
            document.add((IBlockElement) element);
        }

        document.getRenderer().close();
        if (showHeaderFooterFinal)
            footerHandler.writeTotal(pdfDocument);
        //pdfDocument.close();
        //pdfDocument.close();
        document.close();

        //pdfWriter.close();
        //HtmlConverter.convertToPdf(htmlSource, baos, converterProperties);
        //ResponseEntity<byte[]> response = new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        pdfServiceLogs.setStatus(1);
        if (applicationService.save(pdfServiceLogs) == null) {
            logger.error("Could not save log object for request initiated by: " + empMast.getEmpName());
            return new ResponseEntity<>("Could not process your request. Please contact the administrator".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
        //return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
        //return response;
    }

    @ResponseBody
    @RequestMapping("/generatePdfForEsign")
    public ResponseEntity<byte[]> generatePdfForEsign(HttpServletRequest request, String theme, String processId,
                                                      @RequestParam(value = "htmlArray[]") String[] htmlArray,
                                                      Integer signatureCount, String signAlignment,
                                                      String signProperties, String remarksRequiredForAll,
                                                      String orientation, Boolean showHeaderFooter,
                                                      Boolean showHeaderDigNicLogo, Boolean showHeaderOffice,
                                                      Boolean showFooterGenBy, Boolean showFooterPageNumber, Boolean showFooterIp,
                                                      Integer leftMargin, Integer rightMargin, Integer bottomMargin, Integer topMargin) {
        String filename = "pdf_for_" + signatureCount + "_esign.pdf";
        if (htmlArray == null || htmlArray.length == 0 || processId == null) {
            logger.error("htmlArray null or empty or processId null");
            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
        }

        for (String html : htmlArray) {
            if (html == null || html.trim().isEmpty()) {
                logger.error("htmlArray conatins an empty or null element");
                return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
            }
        }


        int htmlArrayLength = htmlArray.length;

        List<String> htmlSourceList = Arrays.asList(htmlArray);
        List<String> htmlSourceWithCssList = new ArrayList<>();


        String clientIp = request.getHeader("X-Real-IP");
        if (clientIp == null || clientIp.isEmpty())
            clientIp = request.getRemoteAddr();

        logger.info("Processing request with process id: " + processId + " Client Ip: " + clientIp);
        PdfServiceEsignLogs pdfServiceEsignLogs = new PdfServiceEsignLogs();
        pdfServiceEsignLogs.setSignType("MULTI");
        pdfServiceEsignLogs.setClientIp(clientIp);
        pdfServiceEsignLogs.setReferer(request.getHeader("referer"));
        pdfServiceEsignLogs.setHost(request.getRemoteHost());
        pdfServiceEsignLogs.setTheme(theme);

        String empCodeDec = PdfSecurity.decrypt(processId, "DiGnIcPrOcEsSiD!");
        logger.info("Decrypted process id(EmpCode): " + empCodeDec);
        if (empCodeDec.isEmpty()) {
            logEsignPdfError(pdfServiceEsignLogs, "Decrypted EmpCode Empty");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        Long empCode = null;
        try {
            empCode = Long.parseLong(empCodeDec);
        } catch (Exception ignored) {

        }
        if (empCode == null) {
            logEsignPdfError(pdfServiceEsignLogs, "Decrypted EmpCode could not be parsed as long");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        empMast = applicationService.getApmDetails(empCode);
        if (empMast == null) {
            logEsignPdfError(pdfServiceEsignLogs, "APM null for EmpCode");
            return new ResponseEntity<>("Invalid Emp Code".getBytes(), HttpStatus.BAD_REQUEST);
        }
        logger.info("Request Initiator: " + empMast.getEmpName());
        pdfServiceEsignLogs.setEmpCode(empMast);

        if (signatureCount == null || signAlignment == null) {
            logEsignPdfError(pdfServiceEsignLogs, "signatureCount or signRemarkRequired or signAlignment null or signatureCount less than 2: " + signAlignment);
            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
        }

        if (!signAlignment.equals(SIGN_ALIGNMENT_RIGHT_TO_LEFT) && !signAlignment.equals(SIGN_ALIGNMENT_LEFT_TO_RIGHT)) {
            logEsignPdfError(pdfServiceEsignLogs, "Sign Alignment invalid: " + signAlignment);
            return new ResponseEntity<>("Invalid signAlignment".getBytes(), HttpStatus.BAD_REQUEST);
        }

        pdfServiceEsignLogs.setSignatureCount(signatureCount);
        pdfServiceEsignLogs.setSignAlignment(signAlignment);
        //pdfServiceEsignLogs.setSignRemarkRequired(signRemarkRequired);

        Map<Integer, SignProperty> signPropertiesParsed = null;
        if (signProperties != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                signPropertiesParsed = objectMapper.readValue(signProperties, new TypeReference<Map<Integer, SignProperty>>() {
                });
            } catch (IOException e) {
                logger.error("IOException", e);
            }
        }

        if (signPropertiesParsed == null || signPropertiesParsed.isEmpty()) {
            signPropertiesParsed = new HashMap<>();
            for (int i = 1; i <= signatureCount; i++) {
                signPropertiesParsed.put(i, new SignProperty(null, SIGN_REMARKS_NONE));
            }
        }
        if (signPropertiesParsed.keySet().stream().anyMatch(key -> key <= 0)) {
            logEsignPdfError(pdfServiceEsignLogs, "signProperties contains 0 or negative values as keys");
            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
        }
        for (int i = 1; i <= signatureCount; i++) {
            final int currIndex = i;
            if (i == 1 || !signPropertiesParsed.containsKey(i) || signPropertiesParsed.get(i) == null) {
                signPropertiesParsed.put(i, new SignProperty(null, SIGN_REMARKS_NONE));
            } else {
                if (signPropertiesParsed.get(i).getParallelSignNumbers() != null) {
                    {
                        if (signPropertiesParsed.get(i).getParallelSignNumbers().size() > 2) {
                            logEsignPdfError(pdfServiceEsignLogs, "signProperties contains invalid number for Parallel Signatures for some signature");
                            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
                        }
                        if (signPropertiesParsed.get(i).getParallelSignNumbers().stream().anyMatch(parallelSignNumbers -> parallelSignNumbers == currIndex || parallelSignNumbers > signatureCount)) {
                            logEsignPdfError(pdfServiceEsignLogs, "signProperties contains invalid values for Parallel Signature Numbers");
                            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
                        }

                        Iterator<Integer> tempIterator = signPropertiesParsed.get(i).getParallelSignNumbers().iterator();
                        /*for (Integer parallelSign : signPropertiesParsed.get(i).getParallelSignNumbers()) {
                            signPropertiesParsed.remove(i);
                        }*/
                        while (tempIterator.hasNext()) {
                            Integer parallel = tempIterator.next();
                            signPropertiesParsed.remove(parallel);
                        }
                        final Map<Integer, SignProperty> temp = signPropertiesParsed;
                        signPropertiesParsed.entrySet().removeIf(entry -> temp.get(currIndex).getParallelSignNumbers().contains(entry.getKey()));

                    }
                    if (!VALID_SIGN_REMARKS.contains(signPropertiesParsed.get(i).getShowRemarks())) {
                        signPropertiesParsed.get(i).setShowRemarks(SIGN_REMARKS_NONE);
                    }
                }

            }
        }

        pdfServiceEsignLogs.setSignProperties(signPropertiesParsed);


        if (signatureCount < (htmlArray.length - 1)) {
            logEsignPdfError(pdfServiceEsignLogs, "Signature Count is less than the minimum allowed");
            return new ResponseEntity<>("Signature Count mismatch".getBytes(), HttpStatus.BAD_REQUEST);
        }

        //Include Font Awesome css

        StringBuilder cssAdminLte = new StringBuilder();
        if (theme != null && !theme.isEmpty()) {
            if (theme.equalsIgnoreCase("wcar")) {
                cssAdminLte.append(getCssString("static\\css\\gentelella\\custom.css"));
                cssAdminLte.append(getCssString("static\\css\\gentelella\\wcar_style.css"));
            } else if (theme.equalsIgnoreCase("digital") || theme.equalsIgnoreCase("adminlte")) {
                cssAdminLte.append(getCssString("static\\css\\AdminLTE.css"));
                cssAdminLte.append(getCssString("static\\css\\_all-skins.css"));
            }
        }
        //ClassPathResource imgFile = new ClassPathResource("static/images/digitalnic_logo.png");
        ClassPathResource imgFile = new ClassPathResource("static/images/dn1.png");
        try {
            //bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            logoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
        } catch (IOException ignored) {

        }
        String cssBootstrap = getCssString("static\\css\\bootstrap.css") +
                getCssString("static\\font-awesome\\css\\font-awesome.css");

        htmlSourceList.forEach(html -> {
            htmlSourceWithCssList.add("<style>" + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + new String(Base64Utils.decodeFromString(html)).replaceAll("col-md", "col-xs"));
        });

        //htmlSource="<style>"+ css.toString()+"div{color:blue!important;}</style>"+htmlSource;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        //String filename = "Report.pdf";
        headers.setContentDispositionFormData("file", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        //ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getHeader("referer")));
        trustAllHosts();
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getRequestURL().toString().replace("http:", "https:")));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(baos);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        pdfDocument.setTagged();
        pdfDocument.setDefaultPageSize(PageSize.A4);

        boolean showHeaderFooterFinal = showHeaderFooter != null && showHeaderFooter;
        pdfServiceEsignLogs.setShowHeaderFooter(showHeaderFooter);
        PageXofY footerHandler = null;
        if (showHeaderFooterFinal) {
            Date fileTimestamp = new Date();
            pdfServiceEsignLogs.setFileTimestamp(fileTimestamp);
            //String header = "National Informatics Centre\nOffice Automation Division[OAD]\nhttps://digital.nic.in\n";

            Header headerHandler = new Header(showHeaderDigNicLogo, showHeaderOffice);


            footerHandler = new PageXofY(/*pdfDocument, */fileTimestamp, showFooterGenBy, showFooterPageNumber, showFooterIp);

            //Assign event-handlers
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);
        }

        Document document;


        if (orientation != null && orientation.equalsIgnoreCase("landscape")) {
            pdfServiceEsignLogs.setOrientation("L");
            document = new Document(pdfDocument, PageSize.A4.rotate());
        } else {
            pdfServiceEsignLogs.setOrientation("P");
            document = new Document(pdfDocument);
        }
        if (showHeaderFooterFinal)
            document.setMargins(75, 35, 75, 35);
        else {
            topMargin = (topMargin != null) ? topMargin : 40;
            rightMargin = (rightMargin != null) ? rightMargin : 35;
            bottomMargin = (bottomMargin != null) ? bottomMargin : 25;
            leftMargin = (leftMargin != null) ? leftMargin : 40;
            document.setMargins(topMargin, rightMargin, bottomMargin, leftMargin);
        }

        /*Document document = HtmlConverter.convertToDocument(htmlSource, pdfDocument, converterProperties);*/


        Set<List<IElement>> elementsSet = new LinkedHashSet<>();

        for (String html : htmlSourceWithCssList) {
            List<IElement> elements;
            try {
                elements = HtmlConverter.convertToElements(html, converterProperties);
                if (elements != null && !elements.isEmpty())
                    elementsSet.add(elements);
            } catch (IOException e) {
                logger.error("IOException", e);
                logEsignPdfError(pdfServiceEsignLogs, "Exception while converting html1 to pdf elements: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
            }
        }


        //int requiredColumns = signRemarkRequired ? 1 : 2;
        int requiredColumns = 5;

        Table table = new Table(requiredColumns);

        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(Border.NO_BORDER);
        Iterator htmlElementsIterator = elementsSet.iterator();
        //int totalIter = htmlArrayLength * 2 - 1;
        int totalIter = htmlArrayLength + signatureCount;


        Cell blankFillCell = createEmptyCell(1, requiredColumns - 1);
        //blankDoubleCell.setWidth(UnitValue.createPercentValue(80));
        Cell blankSingleCell = createEmptyCell();
        //blankSingleCell.setWidth(UnitValue.createPercentValue(80));
        for (int j = 0, signNo = 1; j < totalIter; j++) {
            if (htmlElementsIterator.hasNext() && j % 2 == 0) {
                Cell cell;
                cell = new Cell(1, requiredColumns);
                cell.setBorder(Border.NO_BORDER);
                List<IElement> elementsList = (List<IElement>) htmlElementsIterator.next();
                for (IElement element : elementsList) {
                    cell.add((IBlockElement) element);
                }
                table.addCell(cell);
                continue;
            }

            SignProperty signProperty = signPropertiesParsed.get(signNo);
            //Case With Remarks
            if (signNo > 1 && (!signProperty.getShowRemarks().equals(SIGN_REMARKS_NONE) || (remarksRequiredForAll != null && SIGN_REMARKS_LENGTH_MAP.keySet().contains(remarksRequiredForAll)))) {
                Cell cell = new Cell(1, requiredColumns);
                cell.setKeepTogether(true);
                cell.setBorder(Border.NO_BORDER);

                String length = remarksRequiredForAll != null && SIGN_REMARKS_LENGTH_MAP.keySet().contains(remarksRequiredForAll) ? remarksRequiredForAll : signProperty.getShowRemarks();

                cell.setHeight(50 + SIGN_REMARKS_LENGTH_MAP.get(length));
                CellRenderer cellRenderer = new MyCellRendererWithRemarks(cell, "", signNo, signAlignment, signProperty.getParallelSignNumbers());
                cell.setNextRenderer(cellRenderer);
                table.addCell(cell);
            } else {
                Cell esignCellMain = createEmptyCell();
                esignCellMain.setNextRenderer(new MyCellRendererWithoutRemarks(esignCellMain, signNo));
                //cell.setWidth(UnitValue.createPercentValue(20));
                if (signAlignment.equals(SIGN_ALIGNMENT_LEFT_TO_RIGHT)) {
                    table.addCell(esignCellMain);
                    if (signProperty.getParallelSignNumbers() == null)
                        table.addCell(blankFillCell);
                    else {
                        Set currParallelSet = new LinkedHashSet(signProperty.getParallelSignNumbers());
                        Iterator<Integer> iterator = currParallelSet.iterator();
                        for (int i = 2; i <= requiredColumns; i++) {
                            if (!iterator.hasNext()) {
                                //Fill Empty Space in row
                                table.addCell(createEmptyCell(1, requiredColumns - i));
                                break;
                            }
                            if (i % 2 == 0) {
                                table.addCell(blankSingleCell);
                            } else {
                                Cell cell = createEmptyCell();
                                cell.setNextRenderer(new MyCellRendererWithoutRemarks(cell, iterator.next()));
                            }
                        }
                    }

                } else {
                    if (signProperty.getParallelSignNumbers() == null)
                        table.addCell(blankFillCell);
                    else {
                        Set currParallelSet = new LinkedHashSet(signProperty.getParallelSignNumbers());
                        Iterator<Integer> iterator = currParallelSet.iterator();
                        for (int i = 2; i <= requiredColumns; i++) {
                            if (!iterator.hasNext()) {
                                //Fill Empty Space in row
                                table.addCell(createEmptyCell(1, requiredColumns - i));
                                break;
                            }
                            if (i % 2 == 0) {
                                table.addCell(blankSingleCell);
                            } else {
                                Cell cell = createEmptyCell();
                                cell.setNextRenderer(new MyCellRendererWithoutRemarks(cell, iterator.next()));
                            }
                        }
                    }
                    table.addCell(esignCellMain);
                }
            }

            signNo++;
        }

        document.add(table);

        document.getRenderer().close();

        if (showHeaderFooterFinal)
            footerHandler.writeTotal(pdfDocument);

        document.close();

        pdfServiceEsignLogs.setStatus(1);
        if (applicationService.save(pdfServiceEsignLogs) == null) {
            logger.error("Could not save log object for request initiated by: " + empMast.getEmpName());
            return new ResponseEntity<>("Could not process your request. Please contact the administrator".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
    }


    @PostMapping(path = "/mergePdfs")
    public ResponseEntity<byte[]> mergePdfs(@RequestParam(value = "pdfs[]") MultipartFile[] files) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(outputStream);
            writer.setSmartMode(true);
            writer.setCompressionLevel(9);
            PdfDocument pdfDocument = new PdfDocument(writer);
            pdfDocument.initializeOutlines();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    PdfReader reader = new PdfReader(file.getInputStream());
                    PdfDocument pdfInnerDoc = new PdfDocument(reader);
                    pdfInnerDoc.copyPagesTo(1, pdfInnerDoc.getNumberOfPages(), pdfDocument, new PdfPageFormCopier());
                    pdfInnerDoc.close();
                }
            }

            pdfDocument.close();
            writer.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/pdf"));
            headers.setContentDispositionFormData("file", "Merged.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(outputStream.toByteArray());
        } catch (Exception e) {
            logger.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
        }
    }


    @PostMapping(path = "/splitPdf")
    public ResponseEntity<byte[]> splitPdf(@RequestParam(value = "pdf") MultipartFile file, Integer startPage, Integer lastPage) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Invalid Request".getBytes());
        }


        ByteArrayOutputStream finalOutputStream = new ByteArrayOutputStream();
        try {

            PdfReader original = new PdfReader(file.getInputStream());
            PdfDocument originalPdfDocument = new PdfDocument(original);

            int numberOfPages = originalPdfDocument.getNumberOfPages();
            if (startPage != null && (startPage < 0 || startPage > numberOfPages)) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Invalid Request".getBytes());
            }

            if (lastPage != null && (lastPage < 0 || lastPage > numberOfPages)) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Invalid Request".getBytes());
            }

            if ((startPage != null && lastPage != null) && startPage > lastPage) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Invalid Request".getBytes());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            if (startPage != null || lastPage != null) {
                PdfWriter writer = new PdfWriter(finalOutputStream);
                writer.setSmartMode(true);
                writer.setCompressionLevel(9);
                PdfDocument pdfDocument = new PdfDocument(writer);
                pdfDocument.initializeOutlines();

                if (startPage == null) {
                    startPage = 1;
                }

                if (lastPage == null) {
                    lastPage = numberOfPages;
                }

                originalPdfDocument.copyPagesTo(startPage, lastPage, pdfDocument, new PdfPageFormCopier());
                Utils.safeClose(pdfDocument);
                Utils.safeClose(writer);
                Utils.safeClose(originalPdfDocument);
                Utils.safeClose(original);
                headers.setContentType(MediaType.parseMediaType("application/pdf"));
                headers.setContentDispositionFormData("file", "Split.pdf");
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentType(MediaType.parseMediaType("application/pdf"))
                        .body(finalOutputStream.toByteArray());
            } else {
                ZipOutputStream zipOut = new ZipOutputStream(finalOutputStream);
                for (int i = 1; i <= numberOfPages; i++) {
                    ByteArrayOutputStream pageBaos = new ByteArrayOutputStream();
                    PdfWriter writer = new PdfWriter(pageBaos);
                    writer.setSmartMode(true);
                    writer.setCompressionLevel(9);
                    PdfDocument pdfDocument = new PdfDocument(writer);
                    pdfDocument.initializeOutlines();
                    originalPdfDocument.copyPagesTo(i, i, pdfDocument, new PdfPageFormCopier());
                    Utils.safeClose(pdfDocument);
                    Utils.safeClose(writer);
                    ZipEntry zipEntry = new ZipEntry("Page" + i + ".pdf");
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(pageBaos.toByteArray());
                    Utils.safeClose(pageBaos);
                }
                Utils.safeClose(zipOut);
                Utils.safeClose(originalPdfDocument);
                Utils.safeClose(original);
                headers.setContentType(MediaType.parseMediaType("application/zip"));
                headers.setContentDispositionFormData("file", "Split.zip");
                return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/zip")).body(finalOutputStream.toByteArray());

            }


        } catch (Exception e) {
            logger.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
        }
    }


    @PostMapping(path = "/imageToPdf")
    public ResponseEntity<byte[]> imageToPdf(@RequestParam(value = "images[]") MultipartFile[] file) {
        if (file.length == 0) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Invalid Request".getBytes());
        }
        ByteArrayOutputStream finalOutputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(finalOutputStream);
        writer.setSmartMode(true);
        writer.setCompressionLevel(9);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);
        document.setMargins(0, 0, 0, 0);
        //pdfDocument.initializeOutlines();
        try {

            for (MultipartFile multipartFile : file) {
                if (multipartFile.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Invalid Request".getBytes());
                }

                ImageData imageData = ImageDataFactory.create(multipartFile.getBytes());
                Image image = new Image(imageData);//.scaleAbsolute(100, 200).setFixedPosition(1, 25, 25);
                document.add(image);

            }

            document.close();
            //Utils.safeClose(pdfDocument);
            Utils.safeClose(writer);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/pdf"));
            headers.setContentDispositionFormData("file", "File.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/pdf"))
                    .body(finalOutputStream.toByteArray());
        } catch (Exception e) {
            logger.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
        }
    }


    private Cell createEmptyCell() {
        return createEmptyCell(1, 1);
    }

    private Cell createEmptyCell(int rowspan, int colspan) {
        Cell cell = new Cell(rowspan, colspan);
        cell.setKeepTogether(true);
        cell.setBorder(Border.NO_BORDER);
        cell.setHeight(50);
        return cell;

    }


    @ResponseBody
    @RequestMapping("/generatePdfForEsignDivided")
    public ResponseEntity<byte[]> generatePdfForEsign(HttpServletRequest request, String theme,
                                                      String htmlSource1, String htmlSource2,
                                                      String processId, String orientation,
                                                      Boolean showHeaderFooter, String signAlignment,
                                                      Integer leftMargin, Integer rightMargin, Integer bottomMargin, Integer topMargin) {
        /*InputStream licenseIS = null;
        try {
            licenseIS = new ClassPathResource("itextkey1547889523707_0.xml").getInputStream();
        } catch (IOException e) {
            logger.error("Exception loading license xml stream", e);
            return new ResponseEntity<>("Error Processing Request".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        LicenseKey.loadLicenseFile(licenseIS);*/
        String filename = "pdf_for_esign.pdf";
        if (htmlSource1 == null || htmlSource1.trim().isEmpty() || htmlSource2 == null || htmlSource2.trim().isEmpty() || processId == null) {
            logger.error("htmlSources or processId null");
            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
        }

        String clientIp = request.getHeader("X-Real-IP");
        if (clientIp == null || clientIp.isEmpty())
            clientIp = request.getRemoteAddr();

        logger.info("Processing request with process id: " + processId + " Client Ip: " + clientIp);
        PdfServiceEsignLogs pdfServiceEsignLogs = new PdfServiceEsignLogs();
        pdfServiceEsignLogs.setSignType("SINGLE");
        pdfServiceEsignLogs.setClientIp(clientIp);
        pdfServiceEsignLogs.setReferer(request.getHeader("referer"));
        pdfServiceEsignLogs.setHost(request.getRemoteHost());
        pdfServiceEsignLogs.setTheme(theme);

        String empCodeDec = PdfSecurity.decrypt(processId, "DiGnIcPrOcEsSiD!");
        logger.info("Decrypted process id(EmpCode): " + empCodeDec);
        if (empCodeDec.isEmpty()) {
            logEsignPdfError(pdfServiceEsignLogs, "Decrypted EmpCode Empty");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        Long empCode = null;
        try {
            empCode = Long.parseLong(empCodeDec);
        } catch (Exception ignored) {

        }
        if (empCode == null) {
            logEsignPdfError(pdfServiceEsignLogs, "Decrypted EmpCode could not be parsed as long");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        empMast = applicationService.getApmDetails(empCode);
        if (empMast == null) {
            logEsignPdfError(pdfServiceEsignLogs, "APM null for EmpCode");
            return new ResponseEntity<>("Invalid Emp Code".getBytes(), HttpStatus.BAD_REQUEST);
        }
        logger.info("Request Initiator: " + empMast.getEmpName());
        pdfServiceEsignLogs.setEmpCode(empMast);

        if (signAlignment == null || (!signAlignment.equals(SIGN_ALIGNMENT_RIGHT_TO_LEFT) && !signAlignment.equals(SIGN_ALIGNMENT_LEFT_TO_RIGHT))) {
            logEsignPdfError(pdfServiceEsignLogs, "Sign Alignment invalid: " + signAlignment);
            return new ResponseEntity<>("Invalid signAlignment".getBytes(), HttpStatus.BAD_REQUEST);
        }

        pdfServiceEsignLogs.setSignAlignment(signAlignment);

        //Include Font Awesome css

        StringBuilder cssAdminLte = new StringBuilder();
        if (theme != null && !theme.isEmpty()) {
            if (theme.equalsIgnoreCase("wcar")) {
                cssAdminLte.append(getCssString("static\\css\\gentelella\\custom.css"));
                cssAdminLte.append(getCssString("static\\css\\gentelella\\wcar_style.css"));
            } else if (theme.equalsIgnoreCase("digital") || theme.equalsIgnoreCase("adminlte")) {
                cssAdminLte.append(getCssString("static\\css\\AdminLTE.css"));
                cssAdminLte.append(getCssString("static\\css\\_all-skins.css"));
            }
        }
        //ClassPathResource imgFile = new ClassPathResource("static/images/digitalnic_logo.png");
        ClassPathResource imgFile = new ClassPathResource("static/images/dn1.png");
        try {
            //bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            logoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
        } catch (IOException ignored) {

        }

        String commonCss = getCssString("static\\css\\common.css");

        String cssBootstrap = getCssString("static\\css\\bootstrap.css") +
                getCssString("static\\font-awesome\\css\\font-awesome.css");
        htmlSource1 = "<style>" + commonCss + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + htmlSource1.replaceAll("col-md", "col-xs");
        htmlSource2 = "<style>" + commonCss + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + htmlSource2.replaceAll("col-md", "col-xs");


        //htmlSource="<style>"+ css.toString()+"div{color:blue!important;}</style>"+htmlSource;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        //String filename = "Report.pdf";
        headers.setContentDispositionFormData("file", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        //ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getHeader("referer")));
        trustAllHosts();
        ConverterProperties converterProperties = new ConverterProperties()
                .setBaseUri(FilenameUtils.getPath(request.getRequestURL().toString().replace("http:", "https:")))
                .setCharset(StandardCharsets.UTF_8.name());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(baos);

        PdfDocument pdfDocument = new PdfDocument(pdfWriter);

        pdfDocument.setTagged();
        pdfDocument.setDefaultPageSize(PageSize.A4);

        boolean showHeaderFooterFinal = showHeaderFooter != null && showHeaderFooter;
        pdfServiceEsignLogs.setShowHeaderFooter(showHeaderFooter);
        PageXofY footerHandler = null;
        if (showHeaderFooterFinal) {
            Date fileTimestamp = new Date();
            pdfServiceEsignLogs.setFileTimestamp(fileTimestamp);
            //String header = "National Informatics Centre\nOffice Automation Division[OAD]\nhttps://digital.nic.in\n";
            Header headerHandler = new Header();


            footerHandler = new PageXofY(/*pdfDocument, */fileTimestamp);

            //Assign event-handlers
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);
        }

        /*if(orientation!=null && orientation.equalsIgnoreCase("landscape")) {
            PageOrientationsEventHandler orientationsEventHandler = new PageOrientationsEventHandler();
            orientationsEventHandler.setOrientation(LANDSCAPE);
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, orientationsEventHandler);
        }*/

        //HtmlConverter.convertToPdf(htmlSource, pdfDocument,converterProperties);
        Document document;


        if (orientation != null && orientation.equalsIgnoreCase("landscape")) {
            pdfServiceEsignLogs.setOrientation("L");
            document = new Document(pdfDocument, PageSize.A4.rotate());
        } else {
            pdfServiceEsignLogs.setOrientation("P");
            document = new Document(pdfDocument);
        }
        if (showHeaderFooterFinal)
            document.setMargins(75, 35, 75, 35);
        else {
            topMargin = (topMargin != null) ? topMargin : 40;
            rightMargin = (rightMargin != null) ? rightMargin : 35;
            bottomMargin = (bottomMargin != null) ? bottomMargin : 25;
            leftMargin = (leftMargin != null) ? leftMargin : 40;
            document.setMargins(topMargin, rightMargin, bottomMargin, leftMargin);
        }

        /*Document document = HtmlConverter.convertToDocument(htmlSource, pdfDocument, converterProperties);*/


        List<IElement> elements1;
        try {
            elements1 = HtmlConverter.convertToElements(htmlSource1, converterProperties);
        } catch (IOException e) {
            logger.error("IOException", e);
            logEsignPdfError(pdfServiceEsignLogs, "Exception while converting html1 to pdf elements: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
        }

        List<IElement> elements2;
        try {
            elements2 = HtmlConverter.convertToElements(htmlSource2, converterProperties);
        } catch (IOException e) {
            logger.error("IOException", e);
            logEsignPdfError(pdfServiceEsignLogs, "Exception while converting html2 to pdf elements: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
        }


        //int tableRowCount = (addPgBreak != null && addPgBreak) ? 1 : 2;


        int requiredColumns = 3;

        Table table = new Table(requiredColumns);

        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(Border.NO_BORDER);
        Cell cell;
        cell = new Cell(1, 3);
        cell.setBorder(Border.NO_BORDER);
        for (IElement element : elements1) {
            cell.add((IBlockElement) element);
            //document.add((IBlockElement) element);
        }
        table.addCell(cell);

        if (signAlignment.equals(SIGN_ALIGNMENT_LEFT_TO_RIGHT)) {
            cell = new Cell();
            cell.setBorder(Border.NO_BORDER);
            cell.setNextRenderer(new MyCellRendererWithoutRemarks(cell, 1));
            cell.setHeight(50);
            cell.setWidth(UnitValue.createPercentValue(20));
            table.addCell(cell);
        } else {
            int signatureCount = 1;
            for (int i = 1; i <= signatureCount + 2; i++) {
                cell = new Cell();
                cell.setKeepTogether(true);
                cell.setBorder(Border.NO_BORDER);
                CellRenderer cellRenderer;
                if (i == (signatureCount + 2)) {
                    cellRenderer = new MyCellRendererWithoutRemarks(cell, signatureCount);
                    cell.setNextRenderer(cellRenderer);
                    cell.setWidth(UnitValue.createPercentValue(20));
                } else {
                    cell.setWidth(UnitValue.createPercentValue(40));
                }
                cell.setHeight(50);
                table.addCell(cell);
            }
        }
        Cell cell3;
        cell3 = new Cell(1, 3);
        cell3.setBorder(Border.NO_BORDER);
        for (IElement element : elements2) {
            cell3.add((IBlockElement) element);
        }
        table.addCell(cell3);
        document.add(table);

        document.getRenderer().close();

        if (showHeaderFooterFinal)
            footerHandler.writeTotal(pdfDocument);


        document.close();


        //pdfWriter.close();
        //HtmlConverter.convertToPdf(htmlSource, baos, converterProperties);
        //ResponseEntity<byte[]> response = new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        pdfServiceEsignLogs.setStatus(1);
        if (applicationService.save(pdfServiceEsignLogs) == null)
            logger.error("Could not save log object for request initiated by: " + empMast.getEmpName());

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
    }


    @ResponseBody
    @RequestMapping("/generatePdfForEsignInParts")
    public ResponseEntity<byte[]> generatePdfForEsign(HttpServletRequest request, String theme,
                                                      @RequestParam(value = "htmlArray[]") String[] htmlArray,
                                                      String processId, String orientation,
                                                      Boolean showHeaderFooter, String signAlignment,
                                                      Boolean includeSignAtEnd,
                                                      Integer leftMargin, Integer rightMargin, Integer bottomMargin, Integer topMargin) {
        String filename = "pdf_for_esign.pdf";
        if (htmlArray == null || htmlArray.length == 0 || processId == null) {
            logger.error("htmlArray null or empty or processId null");
            return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
        }

        for (String html : htmlArray) {
            if (html == null || html.trim().isEmpty()) {
                logger.error("htmlArray conatins an empty or null element");
                return new ResponseEntity<>("Invalid Request".getBytes(), HttpStatus.BAD_REQUEST);
            }
        }

        int htmlArrayLength = htmlArray.length;

        List<String> htmlSourceList = Arrays.asList(htmlArray);
        List<String> htmlSourceWithCssList = new ArrayList<>();


        logger.info("Processing request with process id: " + processId + " Client Ip: " + request.getRemoteAddr());
        PdfServiceEsignLogs pdfServiceEsignLogs = new PdfServiceEsignLogs();
        pdfServiceEsignLogs.setSignType("MULTI_BW_HTML");
        pdfServiceEsignLogs.setClientIp(request.getRemoteAddr());
        pdfServiceEsignLogs.setReferer(request.getHeader("referer"));
        pdfServiceEsignLogs.setHost(request.getRemoteHost());
        pdfServiceEsignLogs.setTheme(theme);
        pdfServiceEsignLogs.setSignatureCount(htmlArrayLength - 1);

        String empCodeDec = PdfSecurity.decrypt(processId, "DiGnIcPrOcEsSiD!");
        logger.info("Decrypted process id(EmpCode): " + empCodeDec);
        if (empCodeDec.isEmpty()) {
            logEsignPdfError(pdfServiceEsignLogs, "Decrypted EmpCode Empty");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        Long empCode = null;
        try {
            empCode = Long.parseLong(empCodeDec);
        } catch (Exception ignored) {

        }
        if (empCode == null) {
            logEsignPdfError(pdfServiceEsignLogs, "Decrypted EmpCode could not be parsed as long");
            return new ResponseEntity<>("Invalid Process Id".getBytes(), HttpStatus.BAD_REQUEST);
        }
        empMast = applicationService.getApmDetails(empCode);
        if (empMast == null) {
            logEsignPdfError(pdfServiceEsignLogs, "APM null for EmpCode");
            return new ResponseEntity<>("Invalid Emp Code".getBytes(), HttpStatus.BAD_REQUEST);
        }
        logger.info("Request Initiator: " + empMast.getEmpName());
        pdfServiceEsignLogs.setEmpCode(empMast);

        if (!signAlignment.equals(SIGN_ALIGNMENT_RIGHT_TO_LEFT) && !signAlignment.equals(SIGN_ALIGNMENT_LEFT_TO_RIGHT)) {
            logEsignPdfError(pdfServiceEsignLogs, "Sign Alignment invalid: " + signAlignment);
            return new ResponseEntity<>("Invalid signAlignment".getBytes(), HttpStatus.BAD_REQUEST);
        }

        pdfServiceEsignLogs.setSignAlignment(signAlignment);

        //Include Font Awesome css

        StringBuilder cssAdminLte = new StringBuilder();
        if (theme != null && !theme.isEmpty()) {
            if (theme.equalsIgnoreCase("wcar")) {
                cssAdminLte.append(getCssString("static\\css\\gentelella\\custom.css"));
                cssAdminLte.append(getCssString("static\\css\\gentelella\\wcar_style.css"));
            } else if (theme.equalsIgnoreCase("digital") || theme.equalsIgnoreCase("adminlte")) {
                cssAdminLte.append(getCssString("static\\css\\AdminLTE.css"));
                cssAdminLte.append(getCssString("static\\css\\_all-skins.css"));
            }
        }
        //ClassPathResource imgFile = new ClassPathResource("static/images/digitalnic_logo.png");
        ClassPathResource imgFile = new ClassPathResource("static/images/dn1.png");
        try {
            //bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            logoBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
        } catch (IOException ignored) {

        }
        String cssBootstrap = getCssString("static\\css\\bootstrap.css") +
                getCssString("static\\font-awesome\\css\\font-awesome.css");
        //htmlSource1 = "<style>" + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + htmlSource1.replaceAll("col-md", "col-xs");
        //htmlSource2 = "<style>" + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + htmlSource2.replaceAll("col-md", "col-xs");

        htmlSourceList.forEach(html -> {
            htmlSourceWithCssList.add("<style>" + cssBootstrap + "</style>" + "<style>" + cssAdminLte.toString() + "</style>" + html.replaceAll("col-md", "col-xs"));
        });


        //htmlSource="<style>"+ css.toString()+"div{color:blue!important;}</style>"+htmlSource;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        //String filename = "Report.pdf";
        headers.setContentDispositionFormData("file", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        //ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getHeader("referer")));
        trustAllHosts();
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(request.getRequestURL().toString().replace("http:", "https:")));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(baos);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        pdfDocument.setTagged();
        pdfDocument.setDefaultPageSize(PageSize.A4);

        boolean showHeaderFooterFinal = showHeaderFooter != null && showHeaderFooter;
        pdfServiceEsignLogs.setShowHeaderFooter(showHeaderFooter);
        PageXofY footerHandler = null;
        if (showHeaderFooterFinal) {
            Date fileTimestamp = new Date();
            pdfServiceEsignLogs.setFileTimestamp(fileTimestamp);
            //String header = "National Informatics Centre\nOffice Automation Division[OAD]\nhttps://digital.nic.in\n";
            Header headerHandler = new Header();


            footerHandler = new PageXofY(/*pdfDocument, */fileTimestamp);

            //Assign event-handlers
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);
        }

        /*if(orientation!=null && orientation.equalsIgnoreCase("landscape")) {
            PageOrientationsEventHandler orientationsEventHandler = new PageOrientationsEventHandler();
            orientationsEventHandler.setOrientation(LANDSCAPE);
            pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, orientationsEventHandler);
        }*/

        //HtmlConverter.convertToPdf(htmlSource, pdfDocument,converterProperties);
        Document document;


        if (orientation != null && orientation.equalsIgnoreCase("landscape")) {
            pdfServiceEsignLogs.setOrientation("L");
            document = new Document(pdfDocument, PageSize.A4.rotate());
        } else {
            pdfServiceEsignLogs.setOrientation("P");
            document = new Document(pdfDocument);
        }
        if (showHeaderFooterFinal)
            document.setMargins(75, 35, 75, 35);
        else {
            topMargin = (topMargin != null) ? topMargin : 40;
            rightMargin = (rightMargin != null) ? rightMargin : 35;
            bottomMargin = (bottomMargin != null) ? bottomMargin : 25;
            leftMargin = (leftMargin != null) ? leftMargin : 40;
            document.setMargins(topMargin, rightMargin, bottomMargin, leftMargin);
        }

        /*Document document = HtmlConverter.convertToDocument(htmlSource, pdfDocument, converterProperties);*/


        Set<List<IElement>> elementsSet = new LinkedHashSet<>();

        for (String html : htmlSourceWithCssList) {
            List<IElement> elements;
            try {
                elements = HtmlConverter.convertToElements(html, converterProperties);
                if (elements != null && !elements.isEmpty())
                    elementsSet.add(elements);
            } catch (IOException e) {
                logger.error("IOException", e);
                logEsignPdfError(pdfServiceEsignLogs, "Exception while converting html1 to pdf elements: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not process the request".getBytes());
            }
        }

        int requiredColumns = 3;

        Table table = new Table(requiredColumns);

        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(Border.NO_BORDER);
        Iterator htmlElementsIterator = elementsSet.iterator();
        int totalIter = htmlArrayLength * 2 - 1;
        if (includeSignAtEnd != null && includeSignAtEnd)
            totalIter++;
        for (int j = 0, signNo = 1; j < totalIter; j++) {
            if (j % 2 == 0) {
                Cell cell;
                cell = new Cell(1, 3);
                cell.setBorder(Border.NO_BORDER);
                List<IElement> elementsList = (List<IElement>) htmlElementsIterator.next();
                for (IElement element : elementsList) {
                    cell.add((IBlockElement) element);
                    //document.add((IBlockElement) element);
                }
                table.addCell(cell);
            } else {
                if (signAlignment.equals(SIGN_ALIGNMENT_LEFT_TO_RIGHT)) {
                    Cell cell = new Cell();
                    cell.setBorder(Border.NO_BORDER);
                    cell.setNextRenderer(new MyCellRendererWithoutRemarks(cell, signNo));
                    cell.setHeight(50);
                    cell.setWidth(UnitValue.createPercentValue(20));
                    table.addCell(cell);
                } else {
                    int signatureCount = 1;
                    for (int i = 1; i <= signatureCount + 2; i++) {
                        Cell cell = new Cell();
                        cell.setKeepTogether(true);
                        cell.setBorder(Border.NO_BORDER);
                        CellRenderer cellRenderer;
                        if (i == (signatureCount + 2)) {
                            cellRenderer = new MyCellRendererWithoutRemarks(cell, signNo);
                            cell.setNextRenderer(cellRenderer);
                            cell.setWidth(UnitValue.createPercentValue(20));
                        } else {
                            cell.setWidth(UnitValue.createPercentValue(40));
                        }
                        cell.setHeight(50);
                        table.addCell(cell);
                    }
                }
                signNo++;
            }
        }


        document.add(table);

        document.getRenderer().close();

        if (showHeaderFooterFinal)
            footerHandler.writeTotal(pdfDocument);


        document.close();

        //pdfWriter.close();
        //ResponseEntity<byte[]> response = new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        pdfServiceEsignLogs.setStatus(1);
        if (applicationService.save(pdfServiceEsignLogs) == null)
            logger.error("Could not save log object for request initiated by: " + empMast.getEmpName());

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
    }









    /*private class MyCellRenderer extends CellRenderer {
        private String remarks;
        private int fieldCount;

        public MyCellRenderer(Cell modelElement, String remarks, int fieldCount) {
            super(modelElement);
            this.remarks = remarks;
            this.fieldCount = fieldCount;
        }


        @Override
        public void draw(DrawContext drawContext) {
            super.draw(drawContext);
            PdfAcroForm form = PdfAcroForm.getAcroForm(drawContext.getDocument(), true);
            Rectangle cellRect = getOccupiedAreaBBox();
            cellRect.applyMargins(0, 0, 20, 0, false);

            int divisor = 1;
            if (remarks != null) {
                divisor = 2;
                Rectangle remarksRectangle = new Rectangle(cellRect);
                remarksRectangle.setHeight(cellRect.getHeight() / divisor);
                remarksRectangle.setY(remarksRectangle.getY() + remarksRectangle.getHeight());
                PdfTextFormField field = PdfFormField.createMultilineText(drawContext.getDocument(), remarksRectangle, "eSignRemarks_" + fieldCount, remarks);
                //field.setReadOnly(true);
                form.addField(field);
            }


            Rectangle signatureRectangle = new Rectangle(cellRect);
            signatureRectangle.setHeight(cellRect.getHeight() / divisor);
            signatureRectangle.setWidth(cellRect.getWidth() / 2);
            signatureRectangle.setX(cellRect.getWidth() / 2);

            PdfSignatureFormField signatureFormField = PdfSignatureFormField.createSignature(drawContext.getDocument(), signatureRectangle);
            signatureFormField.setFieldName("eSignSignature_" + fieldCount);
            form.addField(signatureFormField);
        }
    }*/

    private class MyCellRendererWithRemarks extends CellRenderer {
        private String remarks;
        private int fieldCount;
        private String signAlignment;
        private List<Integer> parallelSignatures;

        private MyCellRendererWithRemarks(Cell modelElement, String remarks, int fieldCount, String signAlignment, List<Integer> parallelSignatures) {
            super(modelElement);
            this.remarks = remarks;
            this.fieldCount = fieldCount;
            this.signAlignment = signAlignment;
            this.signAlignment = signAlignment;
            this.parallelSignatures = parallelSignatures;
        }


        @Override
        public void draw(DrawContext drawContext) {
            super.draw(drawContext);
            PdfAcroForm form = PdfAcroForm.getAcroForm(drawContext.getDocument(), true);
            Rectangle cellRect = getOccupiedAreaBBox();
            cellRect.applyMargins(0, 0, 20, 0, false);

            if (remarks != null) {
                Rectangle remarksRectangle = new Rectangle(cellRect);
                remarksRectangle.setHeight(cellRect.getHeight() - 50);
                remarksRectangle.setY(remarksRectangle.getY() + remarksRectangle.getHeight());
                PdfTextFormField field = PdfFormField.createMultilineText(drawContext.getDocument(), remarksRectangle, "eSignRemarks_" + fieldCount, remarks);
                field.setReadOnly(true);
                field.setVisibility(PdfFormField.VISIBLE);
                form.addField(field);
            }

            List<Rectangle> parallelRectangles = null;
            if (parallelSignatures != null && !parallelSignatures.isEmpty()) {
                parallelRectangles = new ArrayList<>();
                for (int i = 0; i < parallelSignatures.size(); i++) {
                    Rectangle signatureRectangle = new Rectangle(cellRect);
                    signatureRectangle.setHeight(50);
                    signatureRectangle.setWidth(cellRect.getWidth() / 5);
                    if (signAlignment.equals(SIGN_ALIGNMENT_RIGHT_TO_LEFT)) {
                        signatureRectangle.setX(2 * i * cellRect.getWidth() / 5);
                    } else {
                        signatureRectangle.setX(2 * (i + 1) * cellRect.getWidth() / 5);
                    }
                    parallelRectangles.add(signatureRectangle);
                }
            }

            Rectangle signatureRectangle = new Rectangle(cellRect);
            signatureRectangle.setHeight(50);
            signatureRectangle.setWidth(cellRect.getWidth() / 5);

            if (signAlignment.equals(SIGN_ALIGNMENT_RIGHT_TO_LEFT)) {
                signatureRectangle.setX(cellRect.getWidth() - cellRect.getWidth() / 5);
            }
            PdfSignatureFormField signatureFormField = PdfSignatureFormField.createSignature(drawContext.getDocument(), signatureRectangle);
            signatureFormField.setFieldName("eSignSignature_" + fieldCount);
            signatureFormField.setReadOnly(true);
            signatureFormField.setVisibility(PdfFormField.VISIBLE);
            form.addField(signatureFormField);

            if (parallelRectangles != null) {
                List<Rectangle> finalParallelRectangles = parallelRectangles;
                parallelRectangles.forEach(rectangle -> {
                    PdfSignatureFormField parallelSignatureFormField = PdfSignatureFormField.createSignature(drawContext.getDocument(), rectangle);
                    parallelSignatureFormField.setFieldName("eSignSignature_" + parallelSignatures.get(finalParallelRectangles.indexOf(rectangle)));
                    parallelSignatureFormField.setReadOnly(true);
                    parallelSignatureFormField.setVisibility(PdfFormField.VISIBLE);
                    form.addField(parallelSignatureFormField);
                });
            }


        }
    }

    private class MyCellRendererWithoutRemarks extends CellRenderer {
        private int fieldCount;

        private MyCellRendererWithoutRemarks(Cell modelElement, int fieldCount) {
            super(modelElement);
            this.fieldCount = fieldCount;
        }


        @Override
        public void draw(DrawContext drawContext) {
            super.draw(drawContext);
            PdfAcroForm form = PdfAcroForm.getAcroForm(drawContext.getDocument(), true);
            Rectangle cellRect = getOccupiedAreaBBox();
            cellRect.applyMargins(0, 0, 20, 0, false);
            PdfSignatureFormField signatureFormField = PdfSignatureFormField.createSignature(drawContext.getDocument(), cellRect);
            signatureFormField.setFieldName("eSignSignature_" + fieldCount);
            signatureFormField.setReadOnly(true);
            signatureFormField.setVisibility(PdfFormField.VISIBLE);
            form.addField(signatureFormField);
        }
    }


    private String getCssString(String path) {
        try {
            Resource resource = resourceLoader.getResource("classpath:\\" + path);
            InputStream is = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String inputLine;
            StringBuilder css = new StringBuilder();

            while ((inputLine = br.readLine()) != null) {
                css.append(inputLine);
            }
            return css.toString();
        } catch (IOException e) {
            logger.error("IOException", e);
            //e.printStackTrace();
            return "";
        }
    }

    /*public static class PageOrientationsEventHandler implements IEventHandler {
        protected PdfNumber orientation = PORTRAIT;

        public void setOrientation(PdfNumber orientation) {
            this.orientation = orientation;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            docEvent.getPage().put(PdfName.Rotate, orientation);
        }
    }*/

    //Header event handler
    protected class Header implements IEventHandler {
        Boolean confidential;
        boolean mirrorLeftRightMargin = false;
        final String header = "DIGITAL NIC\nTool to empower NICians\ndigital.nic.in";
        Boolean showHeaderDigNicLogo, showHeaderOffice;

        private Header() {
        }

        private Header(Boolean showHeaderDigNicLogo, Boolean showHeaderOffice) {
            this.showHeaderDigNicLogo = showHeaderDigNicLogo;
            this.showHeaderOffice = showHeaderOffice;
        }

        private Header(Boolean confidential) {
            this.confidential = confidential;
        }

        @Override
        public void handleEvent(Event event) {
            //Retrieve document and
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(
                    page.getLastContentStream(), page.getResources(), pdf);

            Canvas canvas = new Canvas(pdfCanvas, pdf, pageSize);
            PdfFont font;
            try {
                font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                canvas.setFont(font);
            } catch (IOException e) {
                logger.error("IOException while setting font to Helvetica", e);
            }
            //canvas.setItalic();
            //Write text at position

            if (confidential != null && confidential) {
                canvas.setFontSize(7f);
                canvas.setItalic();
                Paragraph paragraph = new Paragraph("CONFIDENTIAL\nFor Individual's Use Only");

                canvas.showTextAligned(paragraph,
                        pageSize.getWidth() - 25,
                        pageSize.getTop() - 50,
                        TextAlignment.RIGHT);
            }


            canvas.setFontSize(8f);

            if (showHeaderDigNicLogo == null || showHeaderDigNicLogo) {
                Paragraph paragraph = new Paragraph(header);
                paragraph.setFontColor(new DeviceRgb(4, 96, 119));
                canvas.showTextAligned(paragraph,
                        58,
                        pageSize.getTop() - 50, TextAlignment.LEFT);
                //Image image = new Image(ImageDataFactory.create(logoBytes)).setWidth(150).setMargins(20, 0, 0, 20);
                Image image = new Image(ImageDataFactory.create(logoBytes)).setWidth(35).setMargins(17, 0, 0, 20);
                canvas.add(image);
            }

            Paragraph linePara = new Paragraph();
            ILineDrawer iLineDrawer = new SolidLine();
            LineSeparator solidLine = new LineSeparator(iLineDrawer);
            solidLine.setWidth(pageSize.getWidth() - 40);
            solidLine.setMarginLeft(20);
            solidLine.setMarginRight(20);
            linePara.add(solidLine);
            canvas.showTextAligned(linePara, 0, pageSize.getTop() - 65, TextAlignment.LEFT);

            if (showHeaderOffice != null && showHeaderOffice) {
                canvas.setFontSize(7f);
                float w = pageSize.getWidth();
                List tabstops = new ArrayList();
                tabstops.add(new TabStop(w / 2, TabAlignment.CENTER));
                tabstops.add(new TabStop(w, TabAlignment.LEFT));
                Paragraph paragraph = new Paragraph()
                        .setMarginTop(-32)
                        .addTabStops(tabstops)
                        .add(new Tab())
                        .add("Government of India")
                        .add(new Tab())
                        .add("\n")
                        .add(new Tab())
                        .add("Ministry of Electronics & Information Technology")
                        .add(new Tab())
                        .add("\n")
                        .add(new Tab())
                        .add("National Informatics Centre")
                        .add(new Tab());
                canvas.add(paragraph);
            }
            pdfCanvas.release();


        }
    }

    //page X of Y
    protected class PageXofY implements IEventHandler {
        protected PdfFormXObject placeholder;
        protected float side = 20;
        protected float x = 300;
        //        protected float y = 25;
        protected float y = 15;
        protected float space = 4.5f;
        protected float descent = 3;

        private Date fileTimestamp;

        Boolean showFooterGenBy, showFooterPageNumber, showFooterIp;

        private PageXofY(/*PdfDocument pdf, */Date fileTimestamp) {
            placeholder =
                    new PdfFormXObject(new Rectangle(0, 0, side, side));
            this.fileTimestamp = fileTimestamp;
        }

        private PageXofY(Date fileTimestamp, Boolean showFooterGenBy, Boolean showFooterPageNumber, Boolean showFooterIp) {
            placeholder =
                    new PdfFormXObject(new Rectangle(0, 0, side, side));
            this.fileTimestamp = fileTimestamp;
            this.showFooterGenBy = showFooterGenBy;
            this.showFooterPageNumber = showFooterPageNumber;
            this.showFooterIp = showFooterIp;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdf.getPageNumber(page);
            Rectangle pageSize = page.getPageSize();
            x = pageSize.getWidth() - 30;
            PdfCanvas pdfCanvas = new PdfCanvas(
                    page.getLastContentStream(), page.getResources(), pdf);
            Canvas canvas = new Canvas(pdfCanvas, pdf, pageSize);
            PdfFont font;
            try {
                font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                canvas.setFont(font);
            } catch (IOException e) {
                logger.error("IOException while setting font to Helvetica", e);
            }
            Paragraph p;
            if (showFooterPageNumber == null || showFooterPageNumber) {
                canvas.setItalic();
                canvas.setFontSize(8f);
                p = new Paragraph()
                        .add("Page ").add(String.valueOf(pageNumber)).add(" of");
                //canvas.showTextAligned(p, x, y, TextAlignment.RIGHT);
                canvas.showTextAligned(p, x, y, TextAlignment.RIGHT);
            }

            if (showFooterGenBy == null || showFooterGenBy) {

                String generationString = "Generated on " + dateFormat.format(fileTimestamp);
                if (showFooterIp == null || showFooterIp) {
                    String clientIp = request.getHeader("X-Real-IP");
                    if (clientIp == null || clientIp.isEmpty())
                        clientIp = request.getRemoteAddr();
                    generationString += " from " + clientIp;
                }
                generationString += " by " + empMast.getEmpTitle() + " " + empMast.getEmpName() + " [" + empMast.getEmpCode() + "]";

                p = new Paragraph()
                        .add(generationString);
                //canvas.showTextAligned(p, x, y, TextAlignment.RIGHT);
                canvas.showTextAligned(p, 30, y, TextAlignment.LEFT);
            }

            Paragraph linePara = new Paragraph();
            ILineDrawer iLineDrawer = new SolidLine();
            LineSeparator solidLine = new LineSeparator(iLineDrawer);
            solidLine.setWidth(pageSize.getWidth() - 40);
            solidLine.setMarginLeft(20);
            solidLine.setMarginRight(20);
            linePara.add(solidLine);
            canvas.showTextAligned(linePara, 0, y + 20, TextAlignment.LEFT);
            pdfCanvas.addXObject(placeholder, x + space, y - descent);
            pdfCanvas.release();
        }

        private void writeTotal(PdfDocument pdf) {
            Canvas canvas = new Canvas(placeholder, pdf);
            PdfFont font;
            try {
                font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                canvas.setFont(font);
            } catch (IOException e) {
                logger.error("IOException while setting font to Helvetica", e);
            }
            //canvas.setItalic();
            canvas.setFontSize(8f);
            canvas.showTextAligned(String.valueOf(pdf.getNumberOfPages()),
                    0, descent, TextAlignment.LEFT);
        }
    }


    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) {
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(DO_NOT_VERIFY);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    final private static HostnameVerifier DO_NOT_VERIFY = (hostname, session) -> true;


    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Autowired
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }
}
