package nic.oad.pdfservice.dao;

import nic.oad.pdfservice.model.domain.digitalnic.PdfServiceEsignLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface PdfServiceEsignLogsRepo extends JpaRepository<PdfServiceEsignLogs, BigInteger> {
}
