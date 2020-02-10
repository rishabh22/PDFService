package nic.oad.pdfservice.service;

import nic.oad.pdfservice.model.domain.EmpMast;
import nic.oad.pdfservice.model.domain.digitalnic.PdfServiceEsignLogs;
import nic.oad.pdfservice.model.domain.digitalnic.PdfServiceLogs;
import nic.oad.pdfservice.model.domain.oad.IpMaster;

import java.util.List;

public interface ApplicationService {
    EmpMast getApmDetails(Long empCode);
    PdfServiceLogs save(PdfServiceLogs pdfServiceLogs);
    PdfServiceEsignLogs save(PdfServiceEsignLogs pdfServiceEsignLogs);
    List<IpMaster> getIpMastersByIpAddress(String ipAddress);
}
