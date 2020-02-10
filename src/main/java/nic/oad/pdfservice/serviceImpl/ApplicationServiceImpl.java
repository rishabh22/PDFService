package nic.oad.pdfservice.serviceImpl;

import nic.oad.pdfservice.dao.EmpMastRepo;
import nic.oad.pdfservice.dao.IpMasterRepo;
import nic.oad.pdfservice.dao.PdfServiceEsignLogsRepo;
import nic.oad.pdfservice.dao.PdfServiceLogsRepo;
import nic.oad.pdfservice.model.domain.EmpMast;
import nic.oad.pdfservice.model.domain.digitalnic.PdfServiceEsignLogs;
import nic.oad.pdfservice.model.domain.digitalnic.PdfServiceLogs;
import nic.oad.pdfservice.model.domain.oad.IpMaster;
import nic.oad.pdfservice.service.ApplicationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    @Resource
    private EmpMastRepo empMastRepo;

    @Resource
    private PdfServiceLogsRepo pdfServiceLogsRepo;

    @Resource
    private PdfServiceEsignLogsRepo pdfServiceEsignLogsRepo;

    @Resource
    private IpMasterRepo ipMasterRepo;

    @Override
    public EmpMast getApmDetails(Long empCode) {
        return empMastRepo.findByEmpCode(empCode);
    }


    @Override
    public PdfServiceLogs save(PdfServiceLogs pdfServiceLogs) {
        return pdfServiceLogsRepo.save(pdfServiceLogs);
    }

    @Override
    public PdfServiceEsignLogs save(PdfServiceEsignLogs pdfServiceEsignLogs) {
        return pdfServiceEsignLogsRepo.save(pdfServiceEsignLogs);
    }

    @Override
    public List<IpMaster> getIpMastersByIpAddress(String ipAddress) {
        return ipMasterRepo.findByPublicIpOrPrivateIp(ipAddress,ipAddress);
    }
}
