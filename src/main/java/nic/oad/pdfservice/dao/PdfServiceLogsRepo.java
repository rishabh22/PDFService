package nic.oad.pdfservice.dao;

import nic.oad.pdfservice.model.domain.digitalnic.PdfServiceLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface PdfServiceLogsRepo extends JpaRepository<PdfServiceLogs, BigInteger> {
}
